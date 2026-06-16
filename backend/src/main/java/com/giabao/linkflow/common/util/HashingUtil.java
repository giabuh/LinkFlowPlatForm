package com.giabao.linkflow.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashingUtil {
    private static final String SHA_256 = "SHA-256";
    private static final HexFormat HEX = HexFormat.of();

    private HashingUtil() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(SHA_256 + " algorithm is unavailable", exception);
        }
    }
}
