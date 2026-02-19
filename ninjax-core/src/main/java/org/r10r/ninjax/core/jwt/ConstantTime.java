package org.r10r.ninjax.core.jwt;

final class ConstantTime {

    static boolean equals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        int diff = a.length ^ b.length;
        int min = Math.min(a.length, b.length);
        for (int i = 0; i < min; i++) {
            diff |= (a[i] ^ b[i]);
        }
        // If lengths differ, still do not early-return; diff already includes that fact.
        return diff == 0;
    }

    private ConstantTime() {}
}
