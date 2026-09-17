package org.r10r.ninjax.core;

import java.util.Arrays;
import java.util.Optional;

/**
 * The SameSite attribute of a cookie. The enum names are exactly the values
 * written into the Set-Cookie header (e.g. "SameSite=Lax").
 */
public enum SameSite {
    Strict, Lax, None;

    /**
     * Parses "Strict", "Lax" or "None" case-insensitively.
     *
     * @return the matching SameSite or empty if the value is unknown
     */
    public static Optional<SameSite> ofString(String value) {
        return Arrays.stream(values())
                .filter(sameSite -> sameSite.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
