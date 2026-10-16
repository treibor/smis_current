package com.smis.security.captcha;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CaptchaTest {
    @Test
    void uninitializedChallengeCannotPass() {
        assertFalse(new CapthaImpl().checkUserAnswer(""));
        assertFalse(new CapthaImpl().checkUserAnswer(null));
    }

    @Test
    void challengeIsConsumedEvenWhenAnswerIsWrong() {
        var captcha = new CapthaImpl();
        captcha.getCaptchaBufferedImage();
        String answer = (String) ReflectionTestUtils.getField(captcha, "genStr");
        assertFalse(captcha.checkUserAnswer("wrong"));
        assertFalse(captcha.checkUserAnswer(answer));
    }

    @Test
    void validAnswerPassesOnlyOnce() {
        var captcha = new CapthaImpl();
        var image = captcha.getCaptchaBufferedImage();
        assertEquals(150, image.getWidth());
        String answer = (String) ReflectionTestUtils.getField(captcha, "genStr");
        assertEquals(6, answer.length());
        assertTrue(captcha.checkUserAnswer(answer));
        assertFalse(captcha.checkUserAnswer(answer));
    }
}
