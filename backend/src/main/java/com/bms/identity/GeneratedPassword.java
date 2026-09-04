package com.bms.identity;

import java.security.SecureRandom;

/**
 * Passwords an owner hands to an assistant. Read aloud or copied by hand, so the
 * alphabet leaves out characters that look alike.
 */
public final class GeneratedPassword {

    private static final String ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneratedPassword() {
    }

    public static String next() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
