package org.ninjax.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NinjaSession {
    
    public Map<String, String> keyValueStore = new HashMap();
    
    
    protected final String NINJA_SESSION_NAME = "NINJA_SESSION";
    
    public NinjaSession() {
    
    }
    
    public Optional<String> get(String key) {
        return Optional.ofNullable(keyValueStore.get(key));
    }

    
    public void put(String key, String value) {
        keyValueStore.put(key, value);
    }
    
}
