package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.junit.jupiter.api.Test;

class LoginPasswordCipherTest {
    private String encrypt(LoginPasswordCipher target, String value, String field) throws Exception {
        var key = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(target.publicKey())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256, new PSource.PSpecified(
                        (target.challenge() + ":" + field).getBytes(StandardCharsets.UTF_8))));
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String[] encryptedCredentials(LoginPasswordCipher target) throws Exception {
        return new String[]{encrypt(target, "tester", "username"),
                encrypt(target, "secret-\u20ac\uD83D\uDD12", "password"), encrypt(target, "ABC123", "captcha")};
    }

    private LoginPasswordCipher.Credentials decrypt(LoginPasswordCipher target, String[] values)
            throws GeneralSecurityException {
        return target.decrypt(values[0], values[1], values[2]);
    }

    @Test
    void decryptsAllFieldsAndRejectsReplayIncludingAfterRenewal() throws Exception {
        var target = new LoginPasswordCipher();
        var encrypted = encryptedCredentials(target);
        var result = decrypt(target, encrypted);
        assertEquals("tester", result.username());
        assertEquals("secret-\u20ac\uD83D\uDD12", result.password());
        assertEquals("ABC123", result.captcha());
        assertThrows(GeneralSecurityException.class, () -> decrypt(target, encrypted));
        target.renewChallenge();
        assertThrows(GeneralSecurityException.class, () -> decrypt(target, encrypted));
        target.renewChallenge();
        assertEquals("tester", decrypt(target, encryptedCredentials(target)).username());
    }

    @Test
    void rejectsPlaintextMalformedTamperedOtherViewAndSwappedFields() throws Exception {
        var target = new LoginPasswordCipher();
        assertThrows(GeneralSecurityException.class, () -> target.decrypt("tester", "password", "ABC123"));
        target.renewChallenge();
        assertThrows(GeneralSecurityException.class, () -> target.decrypt("!".repeat(344), null, null));
        target.renewChallenge();
        var encrypted = encryptedCredentials(target);
        byte[] tampered = Base64.getDecoder().decode(encrypted[1]);
        tampered[20] ^= 1;
        String[] altered = encrypted.clone();
        altered[1] = Base64.getEncoder().encodeToString(tampered);
        assertThrows(GeneralSecurityException.class, () -> decrypt(target, altered));
        var other = new LoginPasswordCipher();
        assertThrows(GeneralSecurityException.class, () -> decrypt(other, encrypted));
        target.renewChallenge();
        var swapped = encryptedCredentials(target);
        assertThrows(GeneralSecurityException.class, () -> target.decrypt(swapped[1], swapped[0], swapped[2]));
    }
}
