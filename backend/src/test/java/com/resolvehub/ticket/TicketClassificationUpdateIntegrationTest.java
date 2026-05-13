package com.resolvehub.ticket;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.audit.repository.AuditLogRepository;
import com.resolvehub.common.security.JwtService;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.dto.UpdateTicketClassificationRequest;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Map;
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
class TicketClassificationUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void staffCanUpdateCategoryAndPriority() throws Exception {
        Organization organization = createOrganization("Acme");
        User customer = createUser(organization, Role.CUSTOMER, "customer@acme.com", "Customer");
        User agent = createUser(organization, Role.AGENT, "agent@acme.com", "Agent");
        Ticket ticket = createTicket(organization, customer, "Classification update");

        UpdateTicketClassificationRequest request = new UpdateTicketClassificationRequest(
                TicketCategory.SECURITY,
                TicketPriority.URGENT
        );

        mockMvc.perform(patch("/api/tickets/{id}/classification", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SECURITY"))
                .andExpect(jsonPath("$.priority").value("URGENT"));
    }

    @Test
    void customerCannotUpdateClassification() throws Exception {
        Organization organization = createOrganization("Beta");
        User customer = createUser(organization, Role.CUSTOMER, "customer@beta.com", "Customer");
        Ticket ticket = createTicket(organization, customer, "Customer cannot classify");

        UpdateTicketClassificationRequest request = new UpdateTicketClassificationRequest(
                TicketCategory.BILLING,
                TicketPriority.HIGH
        );

        mockMvc.perform(patch("/api/tickets/{id}/classification", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdateTicketFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");
        User managerA = createUser(orgA, Role.MANAGER, "manager@orga.com", "Manager A");
        Ticket ticketB = createTicket(orgB, customerB, "Cross-org classification");

        UpdateTicketClassificationRequest request = new UpdateTicketClassificationRequest(
                TicketCategory.TECHNICAL,
                TicketPriority.HIGH
        );

        mockMvc.perform(patch("/api/tickets/{id}/classification", ticketB.getId())
                        .header("Authorization", bearerToken(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void auditLogIsCreatedWithOldAndNewValues() throws Exception {
        Organization organization = createOrganization("Gamma");
        User customer = createUser(organization, Role.CUSTOMER, "customer@gamma.com", "Customer");
        User admin = createUser(organization, Role.ADMIN, "admin@gamma.com", "Admin");
        Ticket ticket = createTicket(organization, customer, "Audit classification");

        UpdateTicketClassificationRequest request = new UpdateTicketClassificationRequest(
                TicketCategory.FEATURE_REQUEST,
                TicketPriority.LOW
        );

        mockMvc.perform(patch("/api/tickets/{id}/classification", ticket.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.getId())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("TICKET_CLASSIFICATION_UPDATED")))
                .andExpect(jsonPath("$[*].details", hasItem(containsString("category=ACCOUNT"))))
                .andExpect(jsonPath("$[*].details", hasItem(containsString("priority=MEDIUM"))))
                .andExpect(jsonPath("$[*].details", hasItem(containsString("category=FEATURE_REQUEST"))))
                .andExpect(jsonPath("$[*].details", hasItem(containsString("priority=LOW"))));
    }

    @Test
    void invalidValuesReturnValidationError() throws Exception {
        Organization organization = createOrganization("Delta");
        User customer = createUser(organization, Role.CUSTOMER, "customer@delta.com", "Customer");
        User manager = createUser(organization, Role.MANAGER, "manager@delta.com", "Manager");
        Ticket ticket = createTicket(organization, customer, "Invalid classification");

        Map<String, Object> invalidPayload = Map.of(
                "category", "NOT_A_CATEGORY",
                "priority", "MEDIUM"
        );

        mockMvc.perform(patch("/api/tickets/{id}/classification", ticket.getId())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    private Organization createOrganization(String name) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setStatus("ACTIVE");
        return organizationRepository.save(organization);
    }

    private User createUser(Organization organization, Role role, String email, String name) {
        User user = new User();
        user.setOrganization(organization);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        user.setRole(role);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    private Ticket createTicket(Organization organization, User requester, String title) {
        Ticket ticket = new Ticket();
        ticket.setOrganization(organization);
        ticket.setRequester(requester);
        ticket.setTitle(title);
        ticket.setDescription("Description for " + title);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCategory(TicketCategory.ACCOUNT);
        ticket.setSlaDueAt(OffsetDateTime.now().plusHours(24));
        return ticketRepository.save(ticket);
    }

    private String bearerToken(User user) {
        ResolveHubUserPrincipal principal = new ResolveHubUserPrincipal(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole()
        );
        return "Bearer " + jwtService.generateToken(principal);
    }
}
