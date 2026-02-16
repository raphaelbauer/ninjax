package com.r10r.ninjax.core.conf;


import com.r10r.ninjax.core.properties.NinjaProperties;
import static com.google.common.truth.Truth.*;
import java.util.Optional;

public class NinjaPropertiesTest {
    
    public NinjaPropertiesTest() {
          
    }


    @org.junit.jupiter.api.Test
    public void testGet() {
        
        NinjaProperties ninjaProperties = new NinjaProperties();
        assertThat(ninjaProperties.get("one")).isEqualTo(Optional.of("test"));
    }
    
}
