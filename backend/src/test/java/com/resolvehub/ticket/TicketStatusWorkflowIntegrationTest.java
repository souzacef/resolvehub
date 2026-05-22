package com.resolvehub.ticket;

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
import com.resolvehub.ticket.dto.UpdateTicketStatusRequest;
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
class TicketStatusWorkflowIntegrationTest {

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
    void agentCannotUpdateStatusOfUnassignedTicket() throws Exception {
        Organization org = createOrganization("Acme");
        User customer = createUser(org, Role.CUSTOMER, "customer@acme.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@acme.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Unassigned status change");

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCannotUpdateStatusOfTicketAssignedToAnotherUser() throws Exception {
        Organization org = createOrganization("Beta");
        User customer = createUser(org, Role.CUSTOMER, "customer@beta.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@beta.com", "Agent");
        User otherAgent = createUser(org, Role.AGENT, "other-agent@beta.com", "Other Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Assigned elsewhere", otherAgent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanUpdateStatusOfTicketAssignedToThemselves() throws Exception {
        Organization org = createOrganization("Gamma");
        User customer = createUser(org, Role.CUSTOMER, "customer@gamma.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@gamma.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Self-assigned status", agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void agentCanMoveInProgressToWaitingCustomerWhenAssignedToSelf() throws Exception {
        Organization org = createOrganization("Delta");
        User customer = createUser(org, Role.CUSTOMER, "customer@delta.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@delta.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.IN_PROGRESS, "Waiting state", agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.WAITING_CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CUSTOMER"));
    }

    @Test
    void agentCanMoveInProgressToResolvedWhenAssignedToSelf() throws Exception {
        Organization org = createOrganization("Epsilon");
        User customer = createUser(org, Role.CUSTOMER, "customer@epsilon.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@epsilon.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.IN_PROGRESS, "Resolve issue", agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void managerAndAdminCanUpdateStatusForOrganizationTicket() throws Exception {
        Organization org = createOrganization("Zeta");
        User customer = createUser(org, Role.CUSTOMER, "customer@zeta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@zeta.com", "Manager");
        User admin = createUser(org, Role.ADMIN, "admin@zeta.com", "Admin");

        Ticket managerTicket = createTicket(org, customer, TicketStatus.OPEN, "Manager close");
        Ticket adminTicket = createTicket(org, customer, TicketStatus.RESOLVED, "Admin close");

        mockMvc.perform(patch("/api/tickets/{id}/status", managerTicket.getId())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.CLOSED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(patch("/api/tickets/{id}/status", adminTicket.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.CLOSED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void customerCanCloseOwnOpenTicket() throws Exception {
        Organization org = createOrganization("Eta");
        User customer = createUser(org, Role.CUSTOMER, "customer@eta.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Close own ticket");

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.CLOSED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void customerCanReopenOwnResolvedTicketToInProgress() throws Exception {
        Organization org = createOrganization("Theta");
        User customer = createUser(org, Role.CUSTOMER, "customer@theta.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.RESOLVED, "Reopen own resolved");

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void customerCannotMoveTicketToResolved() throws Exception {
        Organization org = createOrganization("Iota");
        User customer = createUser(org, Role.CUSTOMER, "customer@iota.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.IN_PROGRESS, "Unauthorized transition");

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.RESOLVED))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidTransitionReturnsBadRequest() throws Exception {
        Organization org = createOrganization("Kappa");
        User customer = createUser(org, Role.CUSTOMER, "customer@kappa.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@kappa.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Invalid workflow", agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.WAITING_CUSTOMER))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closedTicketCannotBeReopened() throws Exception {
        Organization org = createOrganization("Lambda");
        User customer = createUser(org, Role.CUSTOMER, "customer@lambda.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@lambda.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.CLOSED, "Closed ticket", agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotUpdateTicketFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");
        User agentA = createUser(orgA, Role.AGENT, "agent@orga.com", "Agent A");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");
        Ticket ticketB = createTicket(orgB, customerB, TicketStatus.OPEN, "Cross-org update");

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketB.getId())
                        .header("Authorization", bearerToken(agentA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingStatusReturnsValidationError() throws Exception {
        Organization org = createOrganization("Mu");
        User customer = createUser(org, Role.CUSTOMER, "customer@mu.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@mu.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Missing status field", agent);

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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

    private Ticket createTicket(Organization organization, User requester, TicketStatus status, String title) {
        return createTicket(organization, requester, status, title, null);
    }

    private Ticket createTicket(
            Organization organization,
            User requester,
            TicketStatus status,
            String title,
            User assignee
    ) {
        Ticket ticket = new Ticket();
        ticket.setOrganization(organization);
        ticket.setRequester(requester);
        ticket.setAssignee(assignee);
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
