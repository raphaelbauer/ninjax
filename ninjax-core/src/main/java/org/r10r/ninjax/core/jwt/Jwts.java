package org.r10r.ninjax.core.jwt;

import javax.crypto.SecretKey;

public final class Jwts {

    private Jwts() {}

    public static JwtBuilder builder() {
        return new JwtBuilder();
    }

    public static JwtParserBuilder parser() {
        return new JwtParserBuilder();
    }

    // ---- Builder API ----

    public static final class JwtBuilder {
        private final java.util.Map<String, Object> claims = new java.util.LinkedHashMap<>();
        private java.util.Date nbf;
        private java.util.Date iat;
        private java.util.Date exp;
        private SecretKey key;

        public JwtBuilder notBefore(java.util.Date date) {
            this.nbf = date;
            return this;
        }

        public JwtBuilder issuedAt(java.util.Date date) {
            this.iat = date;
            return this;
        }

        public JwtBuilder expiration(java.util.Date date) {
            this.exp = date;
            return this;
        }

        /** Replaces/sets claims (like jjwt). */
        public JwtBuilder claims(java.util.Map<String, ?> map) {
            this.claims.clear();
            if (map != null) this.claims.putAll(map);
            return this;
        }

        public JwtBuilder signWith(SecretKey key) {
            this.key = key;
            return this;
        }

        public String compact() {
            if (key == null) {
                throw new JwtException("Missing signing key");
            }

            // Add standard registered claims if set. We store numeric dates as seconds since epoch (JWT standard).
            if (nbf != null) claims.put("nbf", nbf.getTime() / 1000L);
            if (iat != null) claims.put("iat", iat.getTime() / 1000L);
            if (exp != null) claims.put("exp", exp.getTime() / 1000L);

            java.util.Map<String, Object> header = new java.util.LinkedHashMap<>();
            header.put("typ", "JWT");
            header.put("alg", "HS256");

            String headerJson = JwtJsonParser.minifyAndSerializeObject(header);
            String payloadJson = JwtJsonParser.minifyAndSerializeObject(claims);

            String headerB64 = Base64Url.encode(headerJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String payloadB64 = Base64Url.encode(payloadJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signingInput = headerB64 + "." + payloadB64;

            byte[] sig = Hmac.hmacSha256(key, signingInput.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            String sigB64 = Base64Url.encode(sig);

            return signingInput + "." + sigB64;
        }
    }

    // ---- Parser API ----

    public static final class JwtParserBuilder {
        private SecretKey verifyKey;

        public JwtParserBuilder verifyWith(SecretKey key) {
            this.verifyKey = key;
            return this;
        }

        public JwtParser build() {
            if (verifyKey == null) throw new JwtException("Missing verification key");
            return new JwtParser(verifyKey);
        }
    }
}
