package com.resolvehub.ticket;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.common.security.JwtService;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.dto.UpdateTicketAssigneeRequest;
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
class TicketAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        ticketCommentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void agentCanAssignUnassignedTicketToSelf() throws Exception {
        Organization org = createOrganization("Acme");
        User customer = createUser(org, Role.CUSTOMER, "customer@acme.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@acme.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Unassigned ticket");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agent.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(agent.getId().toString()));
    }

    @Test
    void agentCannotAssignTicketToAnotherUser() throws Exception {
        Organization org = createOrganization("Beta");
        User customer = createUser(org, Role.CUSTOMER, "customer@beta.com", "Customer");
        User agentA = createUser(org, Role.AGENT, "agent-a@beta.com", "Agent A");
        User agentB = createUser(org, Role.AGENT, "agent-b@beta.com", "Agent B");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Assign rule");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agentB.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(agentA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanAssignTicketToAgentInSameOrganization() throws Exception {
        Organization org = createOrganization("Gamma");
        User customer = createUser(org, Role.CUSTOMER, "customer@gamma.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@gamma.com", "Manager");
        User agent = createUser(org, Role.AGENT, "agent@gamma.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Manager assignment");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agent.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(agent.getId().toString()));
    }

    @Test
    void adminCanUnassignTicket() throws Exception {
        Organization org = createOrganization("Delta");
        User customer = createUser(org, Role.CUSTOMER, "customer@delta.com", "Customer");
        User admin = createUser(org, Role.ADMIN, "admin@delta.com", "Admin");
        User agent = createUser(org, Role.AGENT, "agent@delta.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Admin unassign");
        ticket.setAssignee(agent);
        ticketRepository.save(ticket);

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(null);

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(nullValue()));
    }

    @Test
    void customerCannotAssignTicket() throws Exception {
        Organization org = createOrganization("Epsilon");
        User customer = createUser(org, Role.CUSTOMER, "customer@epsilon.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@epsilon.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Customer cannot assign");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agent.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotAssignCustomerAsAssignee() throws Exception {
        Organization org = createOrganization("Zeta");
        User customer = createUser(org, Role.CUSTOMER, "customer@zeta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@zeta.com", "Manager");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Invalid assignee role");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(customer.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotAssignUserFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");
        User customerA = createUser(orgA, Role.CUSTOMER, "customer@orga.com", "Customer A");
        User managerA = createUser(orgA, Role.MANAGER, "manager@orga.com", "Manager A");
        User agentB = createUser(orgB, Role.AGENT, "agent@orgb.com", "Agent B");
        Ticket ticket = createTicket(orgA, customerA, TicketStatus.OPEN, "Cross-org assignee");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agentB.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotAssignClosedTicket() throws Exception {
        Organization org = createOrganization("Eta");
        User customer = createUser(org, Role.CUSTOMER, "customer@eta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@eta.com", "Manager");
        User agent = createUser(org, Role.AGENT, "agent@eta.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.CLOSED, "Closed assignment");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agent.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignmentUpdatesTicketResponse() throws Exception {
        Organization org = createOrganization("Theta");
        User customer = createUser(org, Role.CUSTOMER, "customer@theta.com", "Customer");
        User admin = createUser(org, Role.ADMIN, "admin@theta.com", "Admin");
        User agent = createUser(org, Role.AGENT, "agent@theta.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Response update");

        UpdateTicketAssigneeRequest request = new UpdateTicketAssigneeRequest(agent.getId());

        mockMvc.perform(patch("/api/tickets/{id}/assignee", ticket.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(agent.getId().toString()));

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(agent.getId().toString()));
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
