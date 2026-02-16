package com.ninjaxframework.core;

public enum Secure {
    Yes, No;

    public static Secure ofBoolean(boolean secure) {
        return secure ? Yes : No;
    }

    public boolean toBoolean() {
        return this == Yes;
    }
}
