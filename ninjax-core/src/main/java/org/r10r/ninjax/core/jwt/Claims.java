package org.r10r.ninjax.core.jwt;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Claims extends LinkedHashMap<String, Object> {

    private Claims() {}

    static Claims from(Map<String, Object> map) {
        Claims c = new Claims();
        if (map != null) c.putAll(map);
        return c;
    }

    public Date getNotBefore() {
        return getNumericDateSeconds("nbf");
    }

    public Date getExpiration() {
        return getNumericDateSeconds("exp");
    }

    private Date getNumericDateSeconds(String name) {
        Object v = get(name);
        if (v == null) return null;

        if (v instanceof Integer i) {
            return new Date(i.longValue() * 1000L);
        }
        if (v instanceof Long l) {
            return new Date(l * 1000L);
        }
        if (v instanceof java.math.BigInteger bi) {
            long sec = bi.longValueExact();
            return new Date(sec * 1000L);
        }
        if (v instanceof Number n) {
            // Reject floating point numeric dates
            double d = n.doubleValue();
            if (d != Math.rint(d)) {
                throw new JwtException("Invalid numeric date (non-integer) for " + name);
            }
            long sec = n.longValue();
            return new Date(sec * 1000L);
        }

        // Reject string dates to avoid permissive ambiguity
        throw new JwtException("Invalid numeric date type for " + name);
    }
}
