package org.r10r.ninjax.core.jwt;


public final class Jws<T> {
    private final T payload;

    Jws(T payload) {
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }
}
