package com.ninjaxframework.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
