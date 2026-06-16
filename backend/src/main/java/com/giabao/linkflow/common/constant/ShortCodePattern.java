package com.giabao.linkflow.common.constant;

/**
 * Allowed shape of a public short code: 6–32 alphanumerics. Shared by the redirect
 * route mapping and the security filter so the access boundary stays in one place.
 */
public final class ShortCodePattern {
    public static final String REGEX = "[A-Za-z0-9]{6,32}";

    private ShortCodePattern() {
    }
}
