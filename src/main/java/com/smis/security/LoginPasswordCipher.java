package com.smis.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/** Per-login-view key; each OAEP label is accepted only once. */
final class LoginPasswordCipher implements java.io.Serializable {
    private final KeyPair keyPair;
    private String challenge;

    LoginPasswordCipher() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            renewChallenge();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot initialize login encryption", e);
        }
    }

    String publicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    String challenge() { return challenge; }

    void renewChallenge() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        challenge = Base64.getEncoder().encodeToString(bytes);
    }

    record Credentials(String username, String password, String captcha) {}

    Credentials decrypt(String username, String password, String captcha) throws GeneralSecurityException {
        String label = challenge;
        challenge = null;
        if (label == null) throw new GeneralSecurityException("Invalid encrypted login");
        return new Credentials(decryptValue(username, label + ":username", 5, 40),
                decryptValue(password, label + ":password", 5, 40),
                decryptValue(captcha, label + ":captcha", 6, 6));
    }

    private String decryptValue(String encoded, String label, int min, int max) throws GeneralSecurityException {
        if (encoded == null || encoded.length() != 344) {
            throw new GeneralSecurityException("Invalid encrypted login");
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(),
                    new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
                            new PSource.PSpecified(label.getBytes(StandardCharsets.UTF_8))));
            String value = new String(cipher.doFinal(Base64.getDecoder().decode(encoded)), StandardCharsets.UTF_8);
            if (value.length() < min || value.length() > max) {
                throw new GeneralSecurityException("Invalid encrypted login");
            }
            return value;
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Invalid encrypted login", e);
        }
    }
}
