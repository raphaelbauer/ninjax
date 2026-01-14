package org.ninja.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


public class Json {

    // NOT ideal as Json is statc and not injected...
    // Especially when it comes to configuration...
    public final static ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }
    
}
