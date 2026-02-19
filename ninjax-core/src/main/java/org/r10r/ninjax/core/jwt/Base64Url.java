package org.r10r.ninjax.core.jwt;

import java.util.Base64;

final class Base64Url {
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    static String encode(byte[] bytes) {
        return ENC.encodeToString(bytes);
    }

    static byte[] decode(String s) {
        return DEC.decode(s);
    }

    private Base64Url() {}
}
