package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.smis.security.captcha.Captcha;
import com.vaadin.flow.component.html.Image;

class LoginTest {
    @Test
    void captchaIsVisibleAndClearFieldsResetsCredentials() {
        Captcha captcha = mock(Captcha.class);
        when(captcha.getCaptchaImg()).thenReturn(new Image());
        Login login = new Login(mock(AuthenticatedUser.class), captcha);
        // Flow value fields register plaintext synchronization; these must remain browser-only.
        assertFalse(com.vaadin.flow.component.HasValue.class.isInstance(login.usernameField));
        assertFalse(com.vaadin.flow.component.HasValue.class.isInstance(login.passwordField));
        assertFalse(com.vaadin.flow.component.HasValue.class.isInstance(login.captchatext));
        login.captcha = mock(Captcha.class);
        when(login.captcha.getCaptchaImg()).thenReturn(new Image());
        login.usernameField.getElement().setProperty("value", "example");
        login.passwordField.getElement().setProperty("value", "incorrect");
        login.button.setEnabled(false);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(login, "clearFields"));

        assertEquals("", login.usernameField.getElement().getProperty("value"));
        assertEquals("", login.passwordField.getElement().getProperty("value"));
        assertTrue(login.button.isEnabled());
        verify(login.captcha).getCaptchaImg();
        assertTrue(login.captchacontainer.getParent().isPresent());
        assertTrue(login.captchatext.getElement().getProperty("required", false));
    }

    @Test
    void clearFieldsRefreshesInitializedCaptcha() {
        Captcha captcha = mock(Captcha.class);
        when(captcha.getCaptchaImg()).thenReturn(new Image());
        Login login = new Login(mock(AuthenticatedUser.class), captcha);
        login.captcha = mock(Captcha.class);
        when(login.captcha.getCaptchaImg()).thenReturn(new Image());
        Image original = new Image();
        Image replacement = new Image();
        when(login.captcha.getCaptchaImg()).thenReturn(original, replacement);
        login.getCaptcha();
        login.captchatext.getElement().setProperty("value", "123456");

        ReflectionTestUtils.invokeMethod(login, "clearFields");

        assertSame(replacement, login.image);
        assertTrue(original.getParent().isEmpty());
        assertEquals(3, login.captchacontainer.getComponentCount());
        assertSame(replacement, login.captchacontainer.getComponentAt(0));
        assertEquals("", login.captchatext.getElement().getProperty("value"));
    }
}
