package com.resolvehub.ticketcomment;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.domain.TicketComment;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketCommentIntegrationTest {

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
    void customerCanAddPublicCommentToOwnTicket() throws Exception {
        Organization org = createOrganization("Acme");
        User customer = createUser(org, Role.CUSTOMER, "customer@acme.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Issue");

        CreateTicketCommentRequest request = new CreateTicketCommentRequest("I need help with this issue", false);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.authorId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.body").value("I need help with this issue"))
                .andExpect(jsonPath("$.internal").value(false));
    }

    @Test
    void customerCannotAddInternalComment() throws Exception {
        Organization org = createOrganization("Beta");
        User customer = createUser(org, Role.CUSTOMER, "customer@beta.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Issue");

        CreateTicketCommentRequest request = new CreateTicketCommentRequest("Private note", true);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotCommentOnAnotherCustomersTicket() throws Exception {
        Organization org = createOrganization("Gamma");
        User customerA = createUser(org, Role.CUSTOMER, "a@gamma.com", "Customer A");
        User customerB = createUser(org, Role.CUSTOMER, "b@gamma.com", "Customer B");
        Ticket ticketB = createTicket(org, customerB, TicketStatus.OPEN, "Other customer issue");

        CreateTicketCommentRequest request = new CreateTicketCommentRequest("Trying to comment", false);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketB.getId())
                        .header("Authorization", bearerToken(customerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotSeeInternalComments() throws Exception {
        Organization org = createOrganization("Delta");
        User customer = createUser(org, Role.CUSTOMER, "customer@delta.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@delta.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Visibility issue");

        createComment(ticket, customer, "Public update", false);
        createComment(ticket, agent, "Internal investigation", true);

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Public update"))
                .andExpect(jsonPath("$[0].internal").value(false));
    }

    @Test
    void agentCanAddInternalComment() throws Exception {
        Organization org = createOrganization("Epsilon");
        User customer = createUser(org, Role.CUSTOMER, "customer@epsilon.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@epsilon.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Agent note");

        CreateTicketCommentRequest request = new CreateTicketCommentRequest("Internal triage details", true);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internal").value(true));
    }

    @Test
    void managerAndAdminCanSeeCommentsInTheirOrganization() throws Exception {
        Organization org = createOrganization("Zeta");
        User customer = createUser(org, Role.CUSTOMER, "customer@zeta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@zeta.com", "Manager");
        User admin = createUser(org, Role.ADMIN, "admin@zeta.com", "Admin");
        User agent = createUser(org, Role.AGENT, "agent@zeta.com", "Agent");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, "Org visibility");

        createComment(ticket, customer, "Customer message", false);
        createComment(ticket, agent, "Internal follow-up", true);

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].internal").value(true));

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].internal").value(true));
    }

    @Test
    void usersCannotAccessCommentsFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");

        User agentA = createUser(orgA, Role.AGENT, "agent@orga.com", "Agent A");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");
        Ticket ticketB = createTicket(orgB, customerB, TicketStatus.OPEN, "Other org ticket");

        CreateTicketCommentRequest request = new CreateTicketCommentRequest("Cross-org attempt", false);

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticketB.getId())
                        .header("Authorization", bearerToken(agentA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticketB.getId())
                        .header("Authorization", bearerToken(agentA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void closedTicketRejectsNewComments() throws Exception {
        Organization org = createOrganization("Eta");
        User customer = createUser(org, Role.CUSTOMER, "customer@eta.com", "Customer");
        Ticket closedTicket = createTicket(org, customer, TicketStatus.CLOSED, "Closed ticket");

        CreateTicketCommentRequest request = new CreateTicketCommentRequest("Please reopen", false);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", closedTicket.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
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

    private TicketComment createComment(Ticket ticket, User author, String body, boolean internal) {
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody(body);
        comment.setInternal(internal);
        return ticketCommentRepository.save(comment);
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
