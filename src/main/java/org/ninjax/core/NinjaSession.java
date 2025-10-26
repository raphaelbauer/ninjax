package org.ninjax.core;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record NinjaSession(ImmutableMap<String, String> keyValueStore) {

    public NinjaSession() {
        this(Map.of());
    }
        
    public NinjaSession(Map<String, String> keyValueStore) {
        this(ImmutableMap.copyOf(keyValueStore));
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(keyValueStore.get(key));
    }

    public NinjaSession withValue(String key, String value) {
        Map<String, String> temp = new HashMap<>(keyValueStore);
        temp.put(key, value);
        return new NinjaSession(ImmutableMap.copyOf(temp));
    }
    
    public NinjaSession removeValue(String key) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Map.Entry<String, String> entry : keyValueStore.entrySet()) {
            if (!entry.getKey().equals(key)) {
                builder.put(entry.getKey(), entry.getValue());
            }
        }
        return new NinjaSession(builder.build());
    }

}
