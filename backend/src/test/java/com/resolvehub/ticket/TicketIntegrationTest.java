package com.resolvehub.ticket;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.resolvehub.ticket.dto.CreateTicketRequest;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.time.OffsetDateTime;
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
class TicketIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

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
    void customerCanCreateTicketForSelf() throws Exception {
        Organization organization = createOrganization("Acme");
        User customer = createUser(organization, Role.CUSTOMER, "customer@acme.com", "Customer One");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Cannot login", TicketPriority.HIGH, TicketCategory.TECHNICAL, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ticketNumber").value(org.hamcrest.Matchers.startsWith("RH-")))
                .andExpect(jsonPath("$.organizationId").value(organization.getId().toString()))
                .andExpect(jsonPath("$.requesterId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.title").value("Cannot login"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category").value("TECHNICAL"));
    }


    @Test
    void ticketNumberIsUniqueAcrossCreatedTickets() throws Exception {
        Organization organization = createOrganization("Acme-Uniq");
        User customer = createUser(organization, Role.CUSTOMER, "unique@acme.com", "Customer Unique");

        var firstResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("First issue", TicketPriority.MEDIUM, TicketCategory.OTHER, null))))
                .andExpect(status().isCreated())
                .andReturn();

        var secondResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Second issue", TicketPriority.MEDIUM, TicketCategory.OTHER, null))))
                .andExpect(status().isCreated())
                .andReturn();

        var firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        var secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        org.junit.jupiter.api.Assertions.assertNotEquals(
                firstJson.get("ticketNumber").asText(),
                secondJson.get("ticketNumber").asText()
        );
        org.junit.jupiter.api.Assertions.assertNotEquals(
                firstJson.get("id").asText(),
                firstJson.get("ticketNumber").asText()
        );
    }

    @Test
    void customerCannotCreateTicketForAnotherUser() throws Exception {
        Organization organization = createOrganization("Beta");
        User customerA = createUser(organization, Role.CUSTOMER, "a@beta.com", "Customer A");
        User customerB = createUser(organization, Role.CUSTOMER, "b@beta.com", "Customer B");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Other user issue", TicketPriority.MEDIUM, TicketCategory.ACCOUNT, customerB.getId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanCreateTicketForCustomerInSameOrganization() throws Exception {
        Organization organization = createOrganization("Gamma");
        User agent = createUser(organization, Role.AGENT, "agent@gamma.com", "Agent");
        User customer = createUser(organization, Role.CUSTOMER, "customer@gamma.com", "Customer");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Phone support issue", TicketPriority.HIGH, TicketCategory.TECHNICAL, customer.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.organizationId").value(organization.getId().toString()));
    }

    @Test
    void managerCanCreateTicketForCustomerInSameOrganization() throws Exception {
        Organization organization = createOrganization("Delta");
        User manager = createUser(organization, Role.MANAGER, "manager@delta.com", "Manager");
        User customer = createUser(organization, Role.CUSTOMER, "customer@delta.com", "Customer");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Billing callback", TicketPriority.MEDIUM, TicketCategory.BILLING, customer.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterId").value(customer.getId().toString()));
    }

    @Test
    void adminCanCreateTicketForCustomerInSameOrganization() throws Exception {
        Organization organization = createOrganization("Epsilon");
        User admin = createUser(organization, Role.ADMIN, "admin@epsilon.com", "Admin");
        User customer = createUser(organization, Role.CUSTOMER, "customer@epsilon.com", "Customer");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Security concern", TicketPriority.URGENT, TicketCategory.SECURITY, customer.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterId").value(customer.getId().toString()));
    }

    @Test
    void staffCannotCreateTicketForCustomerFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");
        User agent = createUser(orgA, Role.AGENT, "agent@orga.com", "Agent");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Cross-org request", TicketPriority.HIGH, TicketCategory.TECHNICAL, customerB.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotCreateTicketForNonCustomerRequester() throws Exception {
        Organization organization = createOrganization("Zeta");
        User agent = createUser(organization, Role.AGENT, "agent@zeta.com", "Agent");
        User manager = createUser(organization, Role.MANAGER, "manager@zeta.com", "Manager");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Invalid requester", TicketPriority.MEDIUM, TicketCategory.ACCOUNT, manager.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffMissingRequesterIdGetsValidationError() throws Exception {
        Organization organization = createOrganization("Eta");
        User agent = createUser(organization, Role.AGENT, "agent@eta.com", "Agent");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketRequest("Missing requester", TicketPriority.MEDIUM, TicketCategory.OTHER, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCanOnlyListAndViewOwnTickets() throws Exception {
        Organization organization = createOrganization("Theta");
        User customerA = createUser(organization, Role.CUSTOMER, "a@theta.com", "Customer A");
        User customerB = createUser(organization, Role.CUSTOMER, "b@theta.com", "Customer B");

        Ticket ticketA = createTicket(organization, customerA, "Issue A");
        Ticket ticketB = createTicket(organization, customerB, "Issue B");

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(customerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ticketA.getId().toString()))
                .andExpect(jsonPath("$[0].ticketNumber").value(org.hamcrest.Matchers.startsWith("RH-")))
                .andExpect(jsonPath("$[0].requesterId").value(customerA.getId().toString()));

        mockMvc.perform(get("/api/tickets/{id}", ticketA.getId())
                        .header("Authorization", bearerToken(customerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketA.getId().toString()))
                .andExpect(jsonPath("$.ticketNumber").value(org.hamcrest.Matchers.startsWith("RH-")));

        mockMvc.perform(get("/api/tickets/{id}", ticketB.getId())
                        .header("Authorization", bearerToken(customerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentCanOnlyAccessTicketsInsideOwnOrganization() throws Exception {
        Organization orgA = createOrganization("Iota");
        Organization orgB = createOrganization("Kappa");

        User agentA = createUser(orgA, Role.AGENT, "agent@iota.com", "Agent A");
        User customerA = createUser(orgA, Role.CUSTOMER, "customer@iota.com", "Customer A");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@kappa.com", "Customer B");

        Ticket ticketA = createTicket(orgA, customerA, "Iota ticket");
        Ticket ticketB = createTicket(orgB, customerB, "Kappa ticket");

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(agentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ticketA.getId().toString()));

        mockMvc.perform(get("/api/tickets/{id}", ticketB.getId())
                        .header("Authorization", bearerToken(agentA)))
                .andExpect(status().isNotFound());
    }

    private CreateTicketRequest createTicketRequest(
            String title,
            TicketPriority priority,
            TicketCategory category,
            java.util.UUID requesterId
    ) {
        return new CreateTicketRequest(
                title,
                "Description for " + title,
                priority,
                category,
                requesterId
        );
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
        ticket.setCategory(TicketCategory.OTHER);
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
