package com.example.brenabishopweighttracker;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Creates and verifies salted PBKDF2 password hashes so raw passwords are never stored.
 */
public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordHasher() {
    }

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS, KEY_LENGTH_BITS);

        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.encodeToString(salt, Base64.NO_WRAP) + "$"
                + Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    public static boolean verifyPassword(String password, String storedValue) {
        if (password == null || storedValue == null) {
            return false;
        }

        String[] parts = storedValue.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] actualHash = derive(password, salt, iterations, expectedHash.length * 8);
            return constantTimeEquals(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean isHashed(String value) {
        return value != null && value.startsWith(PREFIX + "$");
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < expected.length; i++) {
            result |= expected[i] ^ actual[i];
        }
        return result == 0;
    }
}
