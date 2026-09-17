package com.smis.security;

import java.nio.charset.StandardCharsets;

/** Shared server-side policy for account creation and password changes. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 15;
    // Match the existing login field's maximum so every new password can be entered there.
    public static final int MAX_LENGTH = 40;
    public static final String DESCRIPTION = "Use 15–40 characters, including uppercase and lowercase letters, "
            + "a number, and a special character.";
    private static final String SPECIAL_CHARACTERS = "!(){}[]:;<>?,@#$%^&*+=_-~`|./'";

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null) return false;
        // The existing creation/change flow trims before hashing; validate that exact value.
        String value = password.trim();
        if (value.codePointCount(0, value.length()) < MIN_LENGTH || value.length() > MAX_LENGTH
                || value.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        boolean lower = false, upper = false, digit = false, special = false;
        for (int codePoint : value.codePoints().toArray()) {
            lower |= Character.isLowerCase(codePoint);
            upper |= Character.isUpperCase(codePoint);
            digit |= Character.isDigit(codePoint);
            special |= SPECIAL_CHARACTERS.indexOf(codePoint) >= 0;
        }
        return lower && upper && digit && special;
    }
}
