package org.r10r.ninjax.core.jwt;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

final class Hmac {
    static byte[] hmacSha256(SecretKey key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new JwtException("HMAC failure", e);
        }
    }

    private Hmac() {}
}
