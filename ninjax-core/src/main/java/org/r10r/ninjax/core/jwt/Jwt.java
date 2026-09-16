package org.r10r.ninjax.core.jwt;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * Minimal JSON Web Tokens (RFC 7519) signed with HMAC SHA-256 ("HS256").
 *
 * <p>A token looks like {@code base64url(header) + "." + base64url(payload) + "." + base64url(signature)}.
 * The header is always {@code {"typ":"JWT","alg":"HS256"}}, the payload is the minified JSON of the claims.
 *
 * <p>This class only signs and verifies. Registered claims such as "exp" or "nbf" are plain values
 * in the claims map and must be checked by the caller.
 */
public final class Jwt {

    // Limits to reduce the DoS surface of untrusted tokens (e.g. from cookies).
    private static final int MAX_TOKEN_CHARS = 8192;     // whole token
    private static final int MAX_B64_PART_CHARS = 6144;  // each base64url part
    private static final int MAX_JSON_CHARS = 4096;      // decoded header or payload JSON

    private static final String HEADER_JSON = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private Jwt() {}

    /**
     * Creates a signed token containing the given claims.
     */
    public static String sign(Map<String, ?> claims, SecretKey key) {
        String headerB64 = BASE64_URL_ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = BASE64_URL_ENCODER.encodeToString(
                JwtJson.serialize(claims).getBytes(StandardCharsets.UTF_8));

        String signingInput = headerB64 + "." + payloadB64;
        return signingInput + "." + BASE64_URL_ENCODER.encodeToString(hmacSha256(signingInput, key));
    }

    /**
     * Verifies the token and returns its claims.
     *
     * @throws JwtException if the token is malformed, too large, uses anything but HS256
     *                      or has an invalid signature.
     */
    public static Map<String, Object> verify(String token, SecretKey key) {
        if (token == null || token.isEmpty()) throw new JwtException("Token is empty");
        if (token.length() > MAX_TOKEN_CHARS) throw new JwtException("Token too large");

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) throw new JwtException("JWT must have 3 parts");
        for (String part : parts) {
            if (part.length() > MAX_B64_PART_CHARS) throw new JwtException("JWT part too large");
        }

        String headerJson = decodeJson(parts[0], "Header");
        String payloadJson = decodeJson(parts[1], "Payload");
        byte[] signature = decodeBase64(parts[2]);

        Map<String, Object> header = JwtJson.parseObject(headerJson);
        if (!"HS256".equals(header.get("alg"))) {
            throw new JwtException("Unsupported alg: " + header.get("alg"));
        }
        // We don't understand any critical header extensions, so we must reject them (RFC 7515, 4.1.11).
        if (header.containsKey("crit")) {
            throw new JwtException("Unsupported critical header: crit");
        }

        // Check the signature before touching the payload JSON, so untrusted payloads are never parsed.
        // MessageDigest.isEqual compares in constant time and does not leak where the bytes differ.
        byte[] expectedSignature = hmacSha256(parts[0] + "." + parts[1], key);
        if (!MessageDigest.isEqual(expectedSignature, signature)) {
            throw new JwtException("Invalid signature");
        }

        return JwtJson.parseObject(payloadJson);
    }

    private static String decodeJson(String base64, String name) {
        String json = new String(decodeBase64(base64), StandardCharsets.UTF_8);
        if (json.length() > MAX_JSON_CHARS) throw new JwtException(name + " JSON too large");
        return json;
    }

    private static byte[] decodeBase64(String base64) {
        try {
            return BASE64_URL_DECODER.decode(base64);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid base64url", e);
        }
    }

    private static byte[] hmacSha256(String signingInput, SecretKey key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new JwtException("HMAC failure", e);
        }
    }
}
