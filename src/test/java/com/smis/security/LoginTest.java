package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.smis.security.captcha.Captcha;
import com.vaadin.flow.component.html.Image;

class LoginTest {
    @Test
    void clearFieldsWithoutCaptchaResetsCredentials() {
        Login login = new Login(mock(AuthenticatedUser.class));
        login.captcha = mock(Captcha.class);
        login.usernameField.setValue("example");
        login.passwordField.setValue("incorrect");
        login.button.setEnabled(false);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(login, "clearFields"));

        assertTrue(login.usernameField.isEmpty());
        assertTrue(login.passwordField.isEmpty());
        assertTrue(login.button.isEnabled());
        verifyNoInteractions(login.captcha);
    }

    @Test
    void clearFieldsRefreshesInitializedCaptcha() {
        Login login = new Login(mock(AuthenticatedUser.class));
        login.captcha = mock(Captcha.class);
        Image original = new Image();
        Image replacement = new Image();
        when(login.captcha.getCaptchaImg()).thenReturn(original, replacement);
        login.getCaptcha();
        login.captchatext.setValue("123456");

        ReflectionTestUtils.invokeMethod(login, "clearFields");

        assertSame(replacement, login.image);
        assertTrue(original.getParent().isEmpty());
        assertEquals(3, login.captchacontainer.getComponentCount());
        assertSame(replacement, login.captchacontainer.getComponentAt(0));
        assertTrue(login.captchatext.isEmpty());
    }
}
