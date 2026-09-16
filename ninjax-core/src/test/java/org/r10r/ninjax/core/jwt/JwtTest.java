package org.r10r.ninjax.core.jwt;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

public class JwtTest {

    /**
     * Token created by the previous jjwt-like implementation (Jwts.builder()...compact()) via
     * NinjaSessionConverter. It must keep verifying so that existing user sessions survive an upgrade.
     */
    public static final String OLD_IMPLEMENTATION_SECRET = "bmluamF4LW9sZC10b2tlbi1maXh0dXJlLXNlY3JldCE=";
    public static final String OLD_IMPLEMENTATION_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
            + ".eyJub3RlIjoic2F5IFwiaGlcIlxuw7zDsSIsInVzZXJuYW1lIjoiYWxpY2UiLCJleHAiOjQxMDI0NDQ4MDAsIm5iZiI6MTc4OTQ5NDU1OCwiaWF0IjoxNzg5NDk0NTU4fQ"
            + ".1gGVmcaR0on_ORRxnX9SeseE0LsnTGvnN1fcPomhOrY";

    private static final String VALID_HEADER_JSON = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";

    private static SecretKey key32Bytes(char fill) {
        return new SecretKeySpec(String.valueOf(fill).repeat(32).getBytes(StandardCharsets.US_ASCII), "HmacSHA256");
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeBase64Url(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    /** Builds a token from raw JSON with a correct HS256 signature, independent of Jwt.sign. */
    private static String signedToken(String headerJson, String payloadJson, SecretKey key) throws Exception {
        String signingInput = base64Url(headerJson) + "." + base64Url(payloadJson);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    @Test
    void signAndVerifyRoundTrip() {
        // given
        SecretKey key = key32Bytes('K');
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("user", "alice");
        claims.put("flag", true);
        claims.put("count", 7);
        claims.put("exp", 1_700_000_060L);

        // when
        String token = Jwt.sign(claims, key);
        Map<String, Object> verified = Jwt.verify(token, key);

        // then
        assertThat(verified).containsExactly("user", "alice", "flag", true, "count", 7, "exp", 1_700_000_060);
    }

    @Test
    void signUsesBase64UrlWithoutPaddingAndMinifiedJson() {
        // given
        SecretKey key = key32Bytes('K');

        // when
        String token = Jwt.sign(Map.of("user", "alice"), key);

        // then
        String[] parts = token.split("\\.");
        assertThat(parts).hasLength(3);
        assertThat(token).doesNotContain("=");
        assertThat(decodeBase64Url(parts[0])).isEqualTo(VALID_HEADER_JSON);
        assertThat(decodeBase64Url(parts[1])).isEqualTo("{\"user\":\"alice\"}");
    }

    @Test
    void signProducesExactlyTheSameTokenAsTheOldImplementation() {
        // given
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(OLD_IMPLEMENTATION_SECRET), "HmacSHA256");
        Map<String, Object> claims = new LinkedHashMap<>();  // same order as in the old token's payload
        claims.put("note", "say \"hi\"\nüñ");
        claims.put("username", "alice");
        claims.put("exp", 4102444800L);
        claims.put("nbf", 1789494558L);
        claims.put("iat", 1789494558L);

        // when
        String token = Jwt.sign(claims, key);

        // then
        assertThat(token).isEqualTo(OLD_IMPLEMENTATION_TOKEN);
    }

    @Test
    void verifyAcceptsTokenFromOldImplementation() {
        // given
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(OLD_IMPLEMENTATION_SECRET), "HmacSHA256");

        // when
        Map<String, Object> claims = Jwt.verify(OLD_IMPLEMENTATION_TOKEN, key);

        // then
        assertThat(claims).containsExactly(
                "note", "say \"hi\"\nüñ",
                "username", "alice",
                "exp", 4102444800L,
                "nbf", 1789494558,
                "iat", 1789494558);
    }

    @Test
    void rejectsInvalidSignature() {
        // given
        String token = Jwt.sign(Map.of("user", "bob"), key32Bytes('A'));

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key32Bytes('B')));

        // then
        assertThat(exception).hasMessageThat().contains("Invalid signature");
    }

    @Test
    void rejectsTamperedPayload() {
        // given
        SecretKey key = key32Bytes('K');
        String[] parts = Jwt.sign(Map.of("role", "user"), key).split("\\.");
        String tampered = parts[0] + "." + base64Url("{\"role\":\"admin\"}") + "." + parts[2];

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(tampered, key));

        // then
        assertThat(exception).hasMessageThat().contains("Invalid signature");
    }

    @Test
    void rejectsEmptySignature() {
        // given
        SecretKey key = key32Bytes('K');
        String[] parts = Jwt.sign(Map.of("user", "bob"), key).split("\\.");
        String withoutSignature = parts[0] + "." + parts[1] + ".";

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(withoutSignature, key));

        // then
        assertThat(exception).hasMessageThat().contains("Invalid signature");
    }

    @Test
    void checksSignatureBeforeParsingPayload() throws Exception {
        // given a payload that is not JSON and a signature made with another key
        String token = signedToken(VALID_HEADER_JSON, "this is not json", key32Bytes('A'));

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key32Bytes('B')));

        // then the signature error wins, so the payload was never parsed
        assertThat(exception).hasMessageThat().contains("Invalid signature");
    }

    @Test
    void rejectsOtherAlgEvenIfSignatureValid() throws Exception {
        // given
        SecretKey key = key32Bytes('K');
        String token = signedToken("{\"typ\":\"JWT\",\"alg\":\"HS512\"}", "{\"user\":\"eve\"}", key);

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key));

        // then
        assertThat(exception).hasMessageThat().contains("Unsupported alg");
    }

    @Test
    void rejectsAlgNone() {
        // given
        String token = base64Url("{\"typ\":\"JWT\",\"alg\":\"none\"}") + "." + base64Url("{\"user\":\"eve\"}") + ".";

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("Unsupported alg");
    }

    @Test
    void rejectsMissingAlg() throws Exception {
        // given
        SecretKey key = key32Bytes('K');
        String token = signedToken("{\"typ\":\"JWT\"}", "{\"user\":\"eve\"}", key);

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key));

        // then
        assertThat(exception).hasMessageThat().contains("Unsupported alg");
    }

    @Test
    void rejectsCritHeaderEvenIfSignatureValid() throws Exception {
        // given
        SecretKey key = key32Bytes('K');
        String token = signedToken("{\"typ\":\"JWT\",\"alg\":\"HS256\",\"crit\":\"x\"}", "{\"user\":\"mallory\"}", key);

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key));

        // then
        assertThat(exception).hasMessageThat().contains("crit");
    }

    @Test
    void rejectsTokenWithWrongNumberOfParts() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify("a.b", key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("3 parts");
    }

    @Test
    void rejectsEmptyToken() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify("", key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("empty");
    }

    @Test
    void rejectsInvalidBase64() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify("#.#.#", key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("base64");
    }

    @Test
    void rejectsTooLargeToken() {
        // given
        String huge = "a".repeat(8200); // MAX_TOKEN_CHARS = 8192

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(huge, key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("Token too large");
    }

    @Test
    void rejectsTooLargePart() {
        // given
        String token = "a".repeat(7000) + ".b.c"; // MAX_B64_PART_CHARS = 6144

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("part too large");
    }

    @Test
    void rejectsTooLargeHeaderJson() {
        // given a header just over MAX_JSON_CHARS = 4096, while the token stays under the other limits
        String bigHeaderJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\",\"x\":\"" + "x".repeat(4300) + "\"}";
        String token = base64Url(bigHeaderJson) + "." + base64Url("{}") + ".AAAA";

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("Header JSON too large");
    }

    @Test
    void rejectsTooLargePayloadJson() {
        // given a payload just over MAX_JSON_CHARS = 4096, while the token stays under the other limits
        String bigPayloadJson = "{\"x\":\"" + "y".repeat(4300) + "\"}";
        String token = base64Url(VALID_HEADER_JSON) + "." + base64Url(bigPayloadJson) + ".AAAA";

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key32Bytes('K')));

        // then
        assertThat(exception).hasMessageThat().contains("Payload JSON too large");
    }

    @Test
    void rejectsInvalidJsonInSignedPayload() throws Exception {
        // given
        SecretKey key = key32Bytes('K');
        String token = signedToken(VALID_HEADER_JSON, "{\"a\":1,\"a\":2}", key);

        // when
        JwtException exception = assertThrows(JwtException.class, () -> Jwt.verify(token, key));

        // then
        assertThat(exception).hasMessageThat().contains("Duplicate JSON key");
    }
}
