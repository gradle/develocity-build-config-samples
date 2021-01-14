package com.gradle;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Hasher {
    private static final MessageDigest SHA_256 = createMessageDigest();

    static String hashValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        byte[] encodedHash = SHA_256.digest(string.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < encodedHash.length / 4; i++) {
            String hex = Integer.toHexString(0xff & encodedHash[i]);
            if (hex.length() == 1) {
                hexString.append("0");
            }
            hexString.append(hex);
        }
        hexString.append("...");
        return hexString.toString();
    }

    private static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Hasher() {}
}
