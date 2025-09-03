package org.ninjax.core;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

public record NinjaSession(ImmutableMap<String, String> keyValueStore) {

    public static final String NINJA_SESSION_NAME = "NINJA_SESSION";

    public NinjaSession(Map<String, String> keyValueStore) {
        this(ImmutableMap.copyOf(keyValueStore));
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(keyValueStore.get(key));
    }

    public NinjaSession withValue(String key, String value) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.putAll(keyValueStore);
        builder.put(key, value);
        return new NinjaSession(builder.build());
    }

}
