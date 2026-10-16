package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

class LoginSessionTest {
    private final SessionRegistryImpl registry = new SessionRegistryImpl();
    private final AuthenticationManager manager = mock(AuthenticationManager.class);
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final com.smis.security.captcha.Captcha captcha = mock(com.smis.security.captcha.Captcha.class);
    private final Login login = createLogin();

    private Login createLogin() {
        when(captcha.getCaptchaImg()).thenAnswer(invocation -> new com.vaadin.flow.component.html.Image());
        return new Login(mock(AuthenticatedUser.class), captcha);
    }

    LoginSessionTest() {
        clearInvocations(captcha);
        when(login.captcha.getCaptchaImg()).thenAnswer(invocation -> new com.vaadin.flow.component.html.Image());
        when(login.captcha.checkUserAnswer(anyString())).thenReturn(true);
        login.sr = registry;
        login.securityRepo = contexts;
        ReflectionTestUtils.setField(login, "authenticationManager", manager);
    }

    @Test
    void invalidCaptchaNeverAuthenticatesOrChangesSessions() {
        when(login.captcha.checkUserAnswer(anyString())).thenReturn(false);
        String before = request.getSession().getId();
        assertThrows(Login.InvalidCaptchaException.class,
                () -> login.authenticateSession("user", "correct", "ABC123", request, response));
        verifyNoInteractions(manager);
        assertEquals(before, request.getSession().getId());
        assertFalse(contexts.containsContext(request));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("", login.captchatext.getElement().getProperty("value"));
    }

    @Test
    void captchaIsCheckedAndRefreshedBeforeCredentials() {
        login.captchatext.getElement().setProperty("value", "ABC123");
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        assertThrows(BadCredentialsException.class,
                () -> login.authenticateSession("user", "wrong", "ABC123", request, response));
        var order = inOrder(login.captcha, manager);
        order.verify(login.captcha).checkUserAnswer("ABC123");
        order.verify(login.captcha).getCaptchaImg();
        order.verify(manager).authenticate(any());
        assertEquals("", login.captchatext.getElement().getProperty("value"));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void wrongPasswordDoesNotExpireVictimOrRotateAttackerSession() {
        var victim = User.withUsername("victim").password("hash").roles("USER").build();
        registry.registerNewSession("victim-session", victim);
        String before = request.getSession().getId();
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> login.authenticateSession("victim", "wrong", "ABC123", request, response));

        assertFalse(registry.getSessionInformation("victim-session").isExpired());
        assertEquals(before, request.getSession().getId());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void successfulLoginRotatesIdPreservesUiAndOnlyExpiresVerifiedAccountsSessions() {
        var principal = User.withUsername("canonical-user").password("hash").roles("USER").build();
        var other = User.withUsername("other").password("hash").roles("USER").build();
        registry.registerNewSession("old-account-session", principal);
        registry.registerNewSession("other-session", other);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        when(manager.authenticate(any())).thenReturn(authentication);
        Object uiState = new Object();
        request.getSession().setAttribute("vaadin-ui-state", uiState);
        String before = request.getSession().getId();

        login.authenticateSession("submitted-alias", "correct", "ABC123", request, response);

        String after = request.getSession().getId();
        assertNotEquals(before, after);
        assertSame(uiState, request.getSession().getAttribute("vaadin-ui-state"));
        assertNull(registry.getSessionInformation(before));
        assertFalse(registry.getSessionInformation(after).isExpired());
        assertEquals(principal, registry.getSessionInformation(after).getPrincipal());
        assertTrue(registry.getSessionInformation("old-account-session").isExpired());
        assertFalse(registry.getSessionInformation("other-session").isExpired());
        assertSame(authentication, contexts.loadDeferredContext(request).get().getAuthentication());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rotationFailureDoesNotExpireExistingSessionsOrSaveAuthentication() {
        var principal = User.withUsername("user").password("hash").roles("USER").build();
        registry.registerNewSession("existing", principal);
        when(manager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        var failingRequest = spy(request);
        doThrow(new IllegalStateException("rotation failed")).when(failingRequest).changeSessionId();

        assertThrows(IllegalStateException.class,
                () -> login.authenticateSession("user", "correct", "ABC123", failingRequest, response));

        assertFalse(registry.getSessionInformation("existing").isExpired());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertFalse(contexts.containsContext(request));
    }
}
