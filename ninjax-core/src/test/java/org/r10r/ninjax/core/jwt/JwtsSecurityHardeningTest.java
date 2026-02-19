package org.r10r.ninjax.core.jwt;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JwtsSecurityHardeningTest {

    private static SecretKey key32Bytes(char fill) {
        byte[] b = new byte[32];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) fill;
        }
        return new SecretKeySpec(b, "HmacSHA256");
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    void roundTripAndClaims() {
        SecretKey key = key32Bytes('K');

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("user", "alice");
        claims.put("role", "admin");
        claims.put("flag", true);
        claims.put("count", 7);

        Date now = new Date(1_700_000_000_000L);

        String jwt = Jwts.builder()
                .issuedAt(now)
                .notBefore(now)
                .expiration(new Date(now.getTime() + 60_000L))
                .claims(claims)
                .signWith(key)
                .compact();

        Claims parsed = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwt)
                .getPayload();

        assertEquals("alice", parsed.get("user"));
        assertEquals("admin", parsed.get("role"));
        assertEquals(true, parsed.get("flag"));
        assertEquals(7, parsed.get("count"));
        assertNotNull(parsed.getNotBefore());
        assertNotNull(parsed.getExpiration());
        assertTrue(parsed.getExpiration().after(parsed.getNotBefore()));
    }

    @Test
    void rejectsInvalidSignature() {
        SecretKey keyA = key32Bytes('A');
        SecretKey keyB = key32Bytes('B');

        String jwt = Jwts.builder()
                .claims(Map.of("user", "bob"))
                .issuedAt(new Date(1_700_000_000_000L))
                .signWith(keyA)
                .compact();

        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(keyB).build().parseSignedClaims(jwt)
        );
        assertTrue(ex.getMessage().contains("Invalid signature"));
    }

    @Test
    void rejectsWrongAlg() {
        SecretKey key = key32Bytes('K');

        String jwt = Jwts.builder()
                .claims(Map.of("user", "eve"))
                .issuedAt(new Date(1_700_000_000_000L))
                .signWith(key)
                .compact();

        String[] parts = jwt.split("\\.");
        String headerJson = new String(Base64Url.decode(parts[0]), StandardCharsets.UTF_8);
        String tamperedHeaderJson = headerJson.replace("\"HS256\"", "\"HS512\"");
        String tamperedHeaderB64 = Base64Url.encode(tamperedHeaderJson.getBytes(StandardCharsets.UTF_8));
        String tampered = tamperedHeaderB64 + "." + parts[1] + "." + parts[2];

        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(key).build().parseSignedClaims(tampered)
        );
        assertTrue(ex.getMessage().contains("Unsupported alg"));
    }

    @Test
    void rejectsCritHeaderEvenIfSignatureValid() {
        SecretKey key = key32Bytes('K');

        String jwt = Jwts.builder()
                .claims(Map.of("user", "mallory"))
                .issuedAt(new Date(1_700_000_000_000L))
                .signWith(key)
                .compact();

        String[] parts = jwt.split("\\.");

        String headerJson = new String(Base64Url.decode(parts[0]), StandardCharsets.UTF_8);
        String headerWithCrit = headerJson.substring(0, headerJson.length() - 1) + ",\"crit\":\"x\"}";
        String headerB64 = Base64Url.encode(headerWithCrit.getBytes(StandardCharsets.UTF_8));

        String signingInput = headerB64 + "." + parts[1];
        byte[] sig = Hmac.hmacSha256(key, signingInput.getBytes(StandardCharsets.US_ASCII));
        String sigB64 = Base64Url.encode(sig);

        String tampered = headerB64 + "." + parts[1] + "." + sigB64;

        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(key).build().parseSignedClaims(tampered)
        );
        assertTrue(ex.getMessage().contains("crit"));
    }

    @Test
    void tokenTooLargeIsRejectedEarly() {
        SecretKey key = key32Bytes('K');

        String huge = repeat('a', 8200); // MAX_TOKEN_CHARS = 8192 in parser
        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(key).build().parseSignedClaims(huge)
        );
        assertTrue(ex.getMessage().contains("Token too large"));
    }

    @Test
    void jwtPartTooLargeIsRejectedEarly() {
        SecretKey key = key32Bytes('K');

        String bigPart = repeat('a', 7000); // MAX_B64_PART_CHARS = 6144 in parser
        String token = bigPart + "." + "b" + "." + "c";

        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        );
        assertTrue(ex.getMessage().contains("part too large"));
    }

    @Test
    void headerJsonTooLargeRejected() {
        SecretKey key = key32Bytes('K');

        // MAX_JSON_CHARS = 4096, keep JWT under MAX_TOKEN_CHARS = 8192
        // Build header just over 4096 chars.
        int headerPad = 4300;
        String bigHeaderJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\",\"x\":\"" + repeat('x', headerPad) + "\"}";

        String headerB64 = Base64Url.encode(bigHeaderJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64Url.encode("{}".getBytes(StandardCharsets.UTF_8));
        String sigB64 = Base64Url.encode(new byte[32]);
        String token = headerB64 + "." + payloadB64 + "." + sigB64;

        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        );
        assertTrue(ex.getMessage().contains("Header JSON too large"),
                "Unexpected message: " + ex.getMessage());
    }

    @Test
    void payloadJsonTooLargeRejected() {
        SecretKey key = key32Bytes('K');

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

        // MAX_JSON_CHARS = 4096; keep JWT under MAX_TOKEN_CHARS = 8192
        int payloadPad = 4300;
        String bigPayloadJson = "{\"x\":\"" + repeat('y', payloadPad) + "\"}";

        String headerB64 = Base64Url.encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64Url.encode(bigPayloadJson.getBytes(StandardCharsets.UTF_8));
        String sigB64 = Base64Url.encode(new byte[32]);
        String token = headerB64 + "." + payloadB64 + "." + sigB64;

        JwtException ex = assertThrows(JwtException.class, ()
                -> Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        );
        assertTrue(ex.getMessage().contains("Payload JSON too large"),
                "Unexpected message: " + ex.getMessage());
    }

    @Test
    void jsonRejectsArrays() {
        JwtException ex = assertThrows(JwtException.class, () -> JwtJsonParser.parseObject("{\"a\":[1,2,3]}"));
        assertTrue(ex.getMessage().toLowerCase().contains("arrays"));
    }

    @Test
    void jsonRejectsDuplicateKeys() {
        JwtException ex = assertThrows(JwtException.class, () -> JwtJsonParser.parseObject("{\"a\":1,\"a\":2}"));
        assertTrue(ex.getMessage().contains("Duplicate JSON key"));
    }

    @Test
    void jsonRejectsDeepNesting() {
        String s = "{}";
        for (int d = 0; d < 12; d++) {
            s = "{\"a\":" + s + "}";
        }
        final var tooLargeObject = s;
        JwtException ex = assertThrows(JwtException.class, () -> JwtJsonParser.parseObject(tooLargeObject));
        assertTrue(ex.getMessage().toLowerCase().contains("nesting too deep"));
    }

    @Test
    void jsonRejectsVeryLongString() {
        String json = "{\"a\":\"" + repeat('z', 3000) + "\"}";
        JwtException ex = assertThrows(JwtException.class, () -> JwtJsonParser.parseObject(json));
        assertTrue(ex.getMessage().toLowerCase().contains("string too long"));
    }

    @Test
    void numericDateRejectsStringExp() {
        SecretKey key = key32Bytes('K');

        String jwt = Jwts.builder()
                .claims(Map.of("user", "a"))
                .issuedAt(new Date(1_700_000_000_000L))
                .expiration(new Date(1_700_000_060_000L))
                .signWith(key)
                .compact();

        String[] parts = jwt.split("\\.");
        String payloadJson = new String(Base64Url.decode(parts[1]), StandardCharsets.UTF_8);

        String tamperedPayloadJson = payloadJson.replaceAll("\"exp\"\\s*:\\s*(\\d+)", "\"exp\":\"$1\"");
        String payloadB64 = Base64Url.encode(tamperedPayloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = parts[0] + "." + payloadB64;
        String sigB64 = Base64Url.encode(Hmac.hmacSha256(key, signingInput.getBytes(StandardCharsets.US_ASCII)));
        String tampered = parts[0] + "." + payloadB64 + "." + sigB64;

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(tampered).getPayload();

        JwtException ex = assertThrows(JwtException.class, claims::getExpiration);
        assertTrue(ex.getMessage().contains("Invalid numeric date type"));
    }

    @Test
    void numericDateRejectsFloatExp() {
        SecretKey key = key32Bytes('K');

        String jwt = Jwts.builder()
                .claims(Map.of("user", "a"))
                .issuedAt(new Date(1_700_000_000_000L))
                .expiration(new Date(1_700_000_060_000L))
                .signWith(key)
                .compact();

        String[] parts = jwt.split("\\.");
        String payloadJson = new String(Base64Url.decode(parts[1]), StandardCharsets.UTF_8);

        String tamperedPayloadJson = payloadJson.replaceAll("\"exp\"\\s*:\\s*(\\d+)", "\"exp\":$1.5");
        String payloadB64 = Base64Url.encode(tamperedPayloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = parts[0] + "." + payloadB64;
        String sigB64 = Base64Url.encode(Hmac.hmacSha256(key, signingInput.getBytes(StandardCharsets.US_ASCII)));
        String tampered = parts[0] + "." + payloadB64 + "." + sigB64;

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(tampered).getPayload();

        JwtException ex = assertThrows(JwtException.class, claims::getExpiration);
        assertTrue(ex.getMessage().toLowerCase().contains("non-integer"));
    }

    @Test
    void expAndNbfAreInterpretedAsSeconds() {
        SecretKey key = key32Bytes('K');

        long nbfSec = 1_700_000_000L;
        long expSec = 1_700_000_100L;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"nbf\":" + nbfSec + ",\"exp\":" + expSec + ",\"user\":\"x\"}";

        String headerB64 = Base64Url.encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64Url.encode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = headerB64 + "." + payloadB64;
        String sigB64 = Base64Url.encode(Hmac.hmacSha256(key, signingInput.getBytes(StandardCharsets.US_ASCII)));
        String token = signingInput + "." + sigB64;

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals(new Date(nbfSec * 1000L), claims.getNotBefore());
        assertEquals(new Date(expSec * 1000L), claims.getExpiration());
    }
}
