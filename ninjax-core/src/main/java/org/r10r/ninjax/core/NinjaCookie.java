package org.r10r.ninjax.core;

import java.util.Optional;

public record NinjaCookie(
        String name,
        String value,
        Optional<String> domain,
        int maxAge,
        Optional<String> path,
        Secure secure,
        HttpOnly httpOnly,
        Optional<SameSite> sameSite) {

    // Separators that RFC 6265 (via RFC 2616 "token") does not allow in a cookie name.
    private static final String NAME_SEPARATORS = "()<>@,;:\\\"/[]?={} \t";

    public static Builder builder(String name, String value) {
        return new Builder(name, value);
    }

    /**
     * Checks that this cookie can be written into a Set-Cookie header as is (RFC 6265). Without this,
     * a value like {@code "x; Domain=evil.example"} would inject its own cookie attributes.
     *
     * <p>Incoming cookies are not validated, because browsers send all kinds of values.
     *
     * @return this cookie
     * @throws IllegalArgumentException if name, value, path or domain contain characters that are not allowed.
     *                                  Encode such values first, e.g. with URLEncoder or Base64.
     */
    public NinjaCookie requireValidForResponse() {
        if (name == null || name.isEmpty() || !name.chars().allMatch(NinjaCookie::isNameChar)) {
            throw new IllegalArgumentException("Invalid cookie name: '" + name + "'");
        }
        if (value != null && !isValidValue(value)) {
            throw new IllegalArgumentException("Invalid characters in value of cookie '" + name
                    + "'. Encode the value first (e.g. URLEncoder or Base64).");
        }
        domain.ifPresent(d -> requireValidAttribute("Domain", d));
        path.ifPresent(p -> requireValidAttribute("Path", p));
        return this;
    }

    private static boolean isNameChar(int c) {
        return c > 0x20 && c < 0x7F && NAME_SEPARATORS.indexOf(c) < 0;
    }

    private static boolean isValidValue(String value) {
        // A value may be wrapped in double quotes. The quotes themselves are allowed then.
        String unquoted = value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
        // cookie-octet: visible US-ASCII except DQUOTE, comma, semicolon and backslash
        return unquoted.chars().allMatch(c -> c > 0x20 && c < 0x7F && c != '"' && c != ',' && c != ';' && c != '\\');
    }

    private void requireValidAttribute(String attributeName, String attributeValue) {
        boolean valid = attributeValue.chars().allMatch(c -> c >= 0x20 && c != 0x7F && c != ';');
        if (!valid) {
            throw new IllegalArgumentException("Invalid characters in " + attributeName + " of cookie '" + name + "'");
        }
    }

    public static final class Builder {

        private final String name;
        private final String value;

        private String domain;
        private int maxAge = -1; // or whatever default you want
        private String path;
        private Secure secure = Secure.No;
        private HttpOnly httpOnly = HttpOnly.No;
        private SameSite sameSite;

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

        public Builder sameSite(SameSite sameSite) {
            this.sameSite = sameSite;
            return this;
        }

        public NinjaCookie build() {
            // Fail where the cookie is created, not later while the response is written.
            return new NinjaCookie(
                    name,
                    value,
                    Optional.ofNullable(domain),
                    maxAge,
                    Optional.ofNullable(path),
                    secure,
                    httpOnly,
                    Optional.ofNullable(sameSite)
            ).requireValidForResponse();
        }
    }
}
