package com.resolvehub.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.auth.dto.LoginRequest;
import com.resolvehub.auth.dto.RegisterRequest;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void registerCreatesOrganizationAndAdminUserWithHashedPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Acme Corp",
                "Alice Admin",
                "admin@acme.com",
                "StrongPass123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("admin@acme.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        var user = userRepository.findByEmail("admin@acme.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("StrongPass123!", user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("StrongPass123!", user.getPasswordHash()));
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Acme Corp",
                "Alice Admin",
                "duplicate@acme.com",
                "StrongPass123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(
                                        "Beta Corp",
                                        "Bob Admin",
                                        "duplicate@acme.com",
                                        "StrongPass456!"
                                )
                        )))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Gamma Corp",
                "Gina Admin",
                "gina@gamma.com",
                "StrongPass123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("gina@gamma.com", "StrongPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Delta Corp",
                "Dana Admin",
                "dana@delta.com",
                "StrongPass123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest invalidLoginRequest = new LoginRequest("dana@delta.com", "WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
