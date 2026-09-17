package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Aa1!", "Aa1!aaaaaaaaaa", "             Aa1!             ",
            "alllowercase123!", "ALLUPPERCASE123!", "MissingDigits!!", "MissingSpecial123"})
    void rejectsShortOrIncompletePasswords(String password) {
        assertFalse(PasswordPolicy.isValid(password));
    }

    @Test
    void enforcesLengthBoundariesOnTheValueThatWillBeHashed() {
        String minimum = "Aa1!" + "a".repeat(11);
        assertTrue(PasswordPolicy.isValid(minimum));
        assertTrue(PasswordPolicy.isValid("  " + minimum + "  "));
        assertTrue(PasswordPolicy.isValid("Aa1!" + "a".repeat(36)));
        assertFalse(PasswordPolicy.isValid("Aa1!" + "a".repeat(37)));
        assertFalse(PasswordPolicy.isValid("Aa1!" + "界".repeat(24)));
    }
}
