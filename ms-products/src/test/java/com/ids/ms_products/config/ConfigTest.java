package com.ids.ms_products.config;

import com.ids.ms_products.security.SecurityConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigTest {

    @Test
    void openApiConfigInstantiatesOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        assertNotNull(config.customOpenAPI());
    }

    @Test
    void timeConfigurationProvidesClock() {
        TimeConfiguration config = new TimeConfiguration();
        assertNotNull(config.applicationClock());
    }

    @Test
    void securityConfigProvidesPasswordEncoder() {
        SecurityConfig config = new SecurityConfig();
        assertNotNull(config.passwordEncoder());
    }
}