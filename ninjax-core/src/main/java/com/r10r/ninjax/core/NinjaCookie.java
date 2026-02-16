package com.r10r.ninjax.core;

import java.util.Optional;

public record NinjaCookie(
        String name,
        String value,
        Optional<String> domain,
        int maxAge,
        Optional<String> path,
        Secure secure,
        HttpOnly httpOnly) {

    public static Builder builder(String name, String value) {
        return new Builder(name, value);
    }

    public static final class Builder {

        private final String name;
        private final String value;

        private String domain;
        private int maxAge = -1; // or whatever default you want
        private String path;
        private Secure secure = Secure.No;
        private HttpOnly httpOnly = HttpOnly.No;

        public Builder(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder secure(boolean secure) {
            this.secure = Secure.ofBoolean(secure);
            return this;
        }

        public Builder secure(Secure secure) {
            this.secure = secure;
            return this;
        }

        public Builder httpOnly(boolean httpOnly) {
            this.httpOnly = HttpOnly.ofBoolean(httpOnly);
            return this;
        }

        public Builder httpOnly(HttpOnly httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        public NinjaCookie build() {
            return new NinjaCookie(
                    name,
                    value,
                    Optional.ofNullable(domain),
                    maxAge,
                    Optional.ofNullable(path),
                    secure,
                    httpOnly
            );
        }
    }
}
