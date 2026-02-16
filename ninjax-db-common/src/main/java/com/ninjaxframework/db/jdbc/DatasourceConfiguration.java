package com.ninjaxframework.db.jdbc;

import java.util.Map;
import java.util.Optional;

public interface DatasourceConfiguration {
    
    Optional<String> get(String propertyName);
    
    Map<String, String> getAllProperties();
    
}