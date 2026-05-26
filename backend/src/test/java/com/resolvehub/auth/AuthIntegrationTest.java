package com.resolvehub.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.audit.repository.AuditLogRepository;
import com.resolvehub.auth.dto.LoginRequest;
import com.resolvehub.auth.dto.RegisterRequest;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String PASSWORD_POLICY_MESSAGE_SNIPPET =
            "uppercase, lowercase, number, special character, and no spaces";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void registerAcceptsPasswordThatMatchesPolicy() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Policy Corp",
                "Pat Admin",
                "pat@policy.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("pat@policy.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        var user = userRepository.findByEmail("pat@policy.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("Password123!", user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Password123!", user.getPasswordHash()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Teste de senha",
            "lowercaseonlypassword",
            "Password!",
            "Password123",
            "Aa1!"
    })
    void registerRejectsPasswordsThatDoNotMatchPolicy(String invalidPassword) throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Weak Corp",
                "Wendy Admin",
                "wendy@weak.com",
                invalidPassword
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    assertTrue(
                            responseBody.contains(PASSWORD_POLICY_MESSAGE_SNIPPET),
                            "Expected password policy validation details in the response body"
                    );
                });
    }

    @Test
    void registerEndpointIsPublicAndNotRejectedWithForbidden() throws Exception {
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
    void duplicateEmailReturnsConflictWithUsefulError() throws Exception {
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
                .andExpect(status().isConflict())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    Throwable resolvedException = result.getResolvedException();

                    boolean bodyContainsMessage = responseBody != null
                            && responseBody.toLowerCase().contains("email already registered");
                    boolean exceptionContainsMessage = resolvedException instanceof ResponseStatusException ex
                            && "Email already registered".equals(ex.getReason());

                    assertTrue(
                            bodyContainsMessage || exceptionContainsMessage,
                            "Expected duplicate-email error details in response body or resolved exception"
                    );
                });
    }

    @Test
    void loginEndpointIsPublicAndNotRejectedWithForbidden() throws Exception {
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

    @Test
    void protectedEndpointStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assertTrue(
                            statusCode == 401 || statusCode == 403,
                            "Expected protected endpoint to require authentication"
                    );
                });
    }
}
