package org.r10r.ninjax.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Key-value data that lives in the NINJA_SESSION cookie between requests.
 *
 * <p>The cookie is a signed JWT. Signed means the client can't change the values without the
 * server noticing. It is NOT encrypted: anyone with the cookie can base64-decode and read every
 * value. Store ids (e.g. a user id), never secrets such as passwords, tokens or personal data.
 */
public record NinjaSession(Map<String, String> keyValueStore) {

    public NinjaSession() {
        this(Map.of());
    }

    public NinjaSession {
        keyValueStore = Map.copyOf(keyValueStore);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(keyValueStore.get(key));
    }

    public NinjaSession withValue(String key, String value) {
        Map<String, String> temp = new HashMap<>(keyValueStore);
        temp.put(key, value);
        return new NinjaSession(Map.copyOf(temp));
    }

    public NinjaSession removeValue(String key) {
        Map<String, String> temp = new HashMap<>(keyValueStore);
        temp.remove(key);
        return new NinjaSession(Map.copyOf(temp));
    }

}
