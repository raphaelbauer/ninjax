/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package org.ninjax.core.conf;


import org.ninjax.core.properties.NinjaProperties;
import static com.google.common.truth.Truth.*;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author ra
 */
public class NinjaPropertiesTest {
    
    public NinjaPropertiesTest() {
        
        
    }


    @org.junit.jupiter.api.Test
    public void testGet() {
        
        NinjaProperties ninjaProperties = new NinjaProperties();
        assertThat(ninjaProperties.get("one")).isEqualTo(Optional.of("test"));
    }
    
}
