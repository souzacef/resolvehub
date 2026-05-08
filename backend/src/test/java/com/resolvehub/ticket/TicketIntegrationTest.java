package com.resolvehub.ticket;

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
        ticketCommentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void customerCanCreateTicket() throws Exception {
        Organization organization = createOrganization("Acme");
        User customer = createUser(organization, Role.CUSTOMER, "customer@acme.com", "Customer One");

        CreateTicketRequest request = new CreateTicketRequest(
                "Cannot login",
                "I cannot access my account after password reset",
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL
        );

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.organizationId").value(organization.getId().toString()))
                .andExpect(jsonPath("$.requesterId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.title").value("Cannot login"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category").value("TECHNICAL"));
    }

    @Test
    void customerCanOnlyListAndViewOwnTickets() throws Exception {
        Organization organization = createOrganization("Beta");
        User customerA = createUser(organization, Role.CUSTOMER, "a@beta.com", "Customer A");
        User customerB = createUser(organization, Role.CUSTOMER, "b@beta.com", "Customer B");

        Ticket ticketA = createTicket(organization, customerA, "Issue A");
        Ticket ticketB = createTicket(organization, customerB, "Issue B");

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(customerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ticketA.getId().toString()))
                .andExpect(jsonPath("$[0].requesterId").value(customerA.getId().toString()));

        mockMvc.perform(get("/api/tickets/{id}", ticketB.getId())
                        .header("Authorization", bearerToken(customerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentCanOnlyAccessTicketsInsideOwnOrganization() throws Exception {
        Organization orgA = createOrganization("Gamma");
        Organization orgB = createOrganization("Delta");

        User agentA = createUser(orgA, Role.AGENT, "agent@gamma.com", "Agent A");
        User customerA = createUser(orgA, Role.CUSTOMER, "customer@gamma.com", "Customer A");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@delta.com", "Customer B");

        Ticket ticketA = createTicket(orgA, customerA, "Gamma ticket");
        Ticket ticketB = createTicket(orgB, customerB, "Delta ticket");

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(agentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ticketA.getId().toString()));

        mockMvc.perform(get("/api/tickets/{id}", ticketB.getId())
                        .header("Authorization", bearerToken(agentA)))
                .andExpect(status().isNotFound());
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
