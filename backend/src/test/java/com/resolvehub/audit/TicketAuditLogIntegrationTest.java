package com.resolvehub.audit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.resolvehub.ticket.dto.TicketResponse;
import com.resolvehub.ticket.dto.UpdateTicketAssigneeRequest;
import com.resolvehub.ticket.dto.UpdateTicketStatusRequest;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.dto.CreateTicketCommentRequest;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketAuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

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
    void ticketCreationCreatesAuditLog() throws Exception {
        Organization org = createOrganization("Acme");
        User customer = createUser(org, Role.CUSTOMER, "customer@acme.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@acme.com", "Manager");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.HIGH, "Create audit test");

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("TICKET_CREATED"));
    }

    @Test
    void statusChangeCreatesAuditLogWithOldAndNewStatusInDetails() throws Exception {
        Organization org = createOrganization("Beta");
        User customer = createUser(org, Role.CUSTOMER, "customer@beta.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@beta.com", "Agent");
        User manager = createUser(org, Role.MANAGER, "manager@beta.com", "Manager");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.MEDIUM, "Status audit test");
        assignTicketToUser(ticket.id(), agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.id())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].action").value("TICKET_STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].details", containsString("OPEN")))
                .andExpect(jsonPath("$[1].details", containsString("IN_PROGRESS")));
    }

    @Test
    void assignmentCreatesAuditLog() throws Exception {
        Organization org = createOrganization("Gamma");
        User customer = createUser(org, Role.CUSTOMER, "customer@gamma.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@gamma.com", "Manager");
        User agent = createUser(org, Role.AGENT, "agent@gamma.com", "Agent");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.MEDIUM, "Assign audit test");

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.id())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketAssigneeRequest(agent.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].action").value("TICKET_ASSIGNED"));
    }

    @Test
    void unassignmentCreatesAuditLog() throws Exception {
        Organization org = createOrganization("Delta");
        User customer = createUser(org, Role.CUSTOMER, "customer@delta.com", "Customer");
        User admin = createUser(org, Role.ADMIN, "admin@delta.com", "Admin");
        User agent = createUser(org, Role.AGENT, "agent@delta.com", "Agent");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.MEDIUM, "Unassign audit test");

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.id())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketAssigneeRequest(agent.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.id())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketAssigneeRequest(null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].action").value("TICKET_UNASSIGNED"));
    }

    @Test
    void commentCreationCreatesAuditLog() throws Exception {
        Organization org = createOrganization("Epsilon");
        User customer = createUser(org, Role.CUSTOMER, "customer@epsilon.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@epsilon.com", "Manager");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.LOW, "Comment audit test");

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.id())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTicketCommentRequest("Need an update", false))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].action").value("COMMENT_ADDED"));
    }

    @Test
    void customerCannotViewAuditLogs() throws Exception {
        Organization org = createOrganization("Zeta");
        User customer = createUser(org, Role.CUSTOMER, "customer@zeta.com", "Customer");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.HIGH, "Customer view forbidden");

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanViewAuditLogsForAllowedTicket() throws Exception {
        Organization org = createOrganization("Eta");
        User customer = createUser(org, Role.CUSTOMER, "customer@eta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@eta.com", "Manager");
        User agent = createUser(org, Role.AGENT, "agent@eta.com", "Agent");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.MEDIUM, "Agent audit visibility");
        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.id())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketAssigneeRequest(agent.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void managerAndAdminCanViewAuditLogsInOrganization() throws Exception {
        Organization org = createOrganization("Theta");
        User customer = createUser(org, Role.CUSTOMER, "customer@theta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@theta.com", "Manager");
        User admin = createUser(org, Role.ADMIN, "admin@theta.com", "Admin");

        TicketResponse ticket = createTicketViaApi(customer, TicketPriority.LOW, "Manager admin visibility");

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticket.id())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void usersCannotViewAuditLogsFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");

        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");
        User managerA = createUser(orgA, Role.MANAGER, "manager@orga.com", "Manager A");

        TicketResponse ticketB = createTicketViaApi(customerB, TicketPriority.MEDIUM, "Cross-org audit");

        mockMvc.perform(get("/api/tickets/{id}/audit-logs", ticketB.id())
                        .header("Authorization", bearerToken(managerA)))
                .andExpect(status().isNotFound());
    }

    private TicketResponse createTicketViaApi(User customer, TicketPriority priority, String title) throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                title,
                "Ticket description",
                priority,
                TicketCategory.TECHNICAL
        );

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TicketResponse.class);
    }

    private void assignTicketToUser(java.util.UUID ticketId, User assignee) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalStateException("Ticket not found for assignment in test"));
        ticket.setAssignee(assignee);
        ticketRepository.save(ticket);
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

    private Ticket createTicket(Organization organization, User requester, TicketStatus status, String title) {
        Ticket ticket = new Ticket();
        ticket.setOrganization(organization);
        ticket.setRequester(requester);
        ticket.setTitle(title);
        ticket.setDescription("Description for " + title);
        ticket.setStatus(status);
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
