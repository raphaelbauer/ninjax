package org.r10r.ninjax.core.jwt;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JwtParser {

    private final SecretKey key;

    // Reasonable defaults for cookies/headers. Override by changing constant if needed.
    private static final int MAX_TOKEN_CHARS = 8192;          // whole JWT string length
    private static final int MAX_JSON_CHARS = 4096;           // header or payload JSON after decode
    private static final int MAX_B64_PART_CHARS = 6144;       // each base64url part length

    JwtParser(SecretKey key) {
        this.key = key;
    }

    public Jws<Claims> parseSignedClaims(String token) {
        if (token == null) throw new JwtException("Token is null");
        if (token.length() == 0) throw new JwtException("Token is empty");
        if (token.length() > MAX_TOKEN_CHARS) throw new JwtException("Token too large");

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) throw new JwtException("JWT must have 3 parts");

        String headerB64 = parts[0];
        String payloadB64 = parts[1];
        String sigB64 = parts[2];

        if (headerB64.length() > MAX_B64_PART_CHARS ||
            payloadB64.length() > MAX_B64_PART_CHARS ||
            sigB64.length() > MAX_B64_PART_CHARS) {
            throw new JwtException("JWT part too large");
        }

        byte[] headerBytes = Base64Url.decode(headerB64);
        byte[] payloadBytes = Base64Url.decode(payloadB64);
        byte[] sigBytes = Base64Url.decode(sigB64);

        String headerJson = new String(headerBytes, StandardCharsets.UTF_8);
        String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

        if (headerJson.length() > MAX_JSON_CHARS) throw new JwtException("Header JSON too large");
        if (payloadJson.length() > MAX_JSON_CHARS) throw new JwtException("Payload JSON too large");

        Map<String, Object> header = JwtJsonParser.parseObject(headerJson);

        // (3) + (general) strict header checks
        Object alg = header.get("alg");
        if (!(alg instanceof String)) throw new JwtException("Missing/invalid alg");
        if (!"HS256".equals(alg)) throw new JwtException("Unsupported alg: " + alg);

        // Reject critical headers we don't understand (good future-proofing)
        if (header.containsKey("crit")) {
            throw new JwtException("Unsupported critical header: crit");
        }

        String signingInput = headerB64 + "." + payloadB64;
        byte[] expected = Hmac.hmacSha256(key, signingInput.getBytes(StandardCharsets.US_ASCII));
        if (!ConstantTime.equals(expected, sigBytes)) {
            throw new JwtException("Invalid signature");
        }

        Map<String, Object> payload = JwtJsonParser.parseObject(payloadJson);
        Claims claims = Claims.from(payload);

        return new Jws<>(claims);
    }
}