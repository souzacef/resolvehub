package com.resolvehub.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "CORS_ALLOWED_ORIGINS=https://resolvehub-frontend.onrender.com, https://preview.resolvehub.example")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigurationEnvOverrideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightAllowsFirstOriginFromCorsAllowedOriginsProperty() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://resolvehub-frontend.onrender.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://resolvehub-frontend.onrender.com"));
    }

    @Test
    void preflightAllowsSecondOriginFromCommaSeparatedCorsAllowedOriginsProperty() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://preview.resolvehub.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://preview.resolvehub.example"));
    }

    @Test
    void preflightRejectsOriginNotInCorsAllowedOriginsProperty() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://not-allowed.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
