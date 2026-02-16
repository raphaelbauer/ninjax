package com.r10r.ninjax.core;

public enum HttpOnly {
    Yes, No;

    public static HttpOnly ofBoolean(boolean httpOnly) {
        return httpOnly ? Yes : No;
    }

    public boolean toBoolean() {
        return this == Yes;
    }
}
