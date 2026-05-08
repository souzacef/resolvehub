package com.resolvehub.ai;

import static org.hamcrest.Matchers.containsString;
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
class TicketAiClassificationIntegrationTest {

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
    void agentCanRequestClassificationForAllowedTicket() throws Exception {
        Organization org = createOrganization("Acme");
        User customer = createUser(org, Role.CUSTOMER, "customer@acme.com", "Customer");
        User agent = createUser(org, Role.AGENT, "agent@acme.com", "Agent");
        Ticket ticket = createTicket(org, customer, "Login issue", "Cannot login and getting error");

        mockMvc.perform(post("/api/tickets/{id}/ai/classification", ticket.getId())
                        .header("Authorization", bearerToken(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedCategory").value("TECHNICAL"))
                .andExpect(jsonPath("$.suggestedPriority").value("HIGH"))
                .andExpect(jsonPath("$.reasoning", containsString("technical")));
    }

    @Test
    void managerAndAdminCanRequestClassificationForOrganizationTicket() throws Exception {
        Organization org = createOrganization("Beta");
        User customer = createUser(org, Role.CUSTOMER, "customer@beta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@beta.com", "Manager");
        User admin = createUser(org, Role.ADMIN, "admin@beta.com", "Admin");
        Ticket ticket = createTicket(org, customer, "Invoice mismatch", "Charged twice this month");

        mockMvc.perform(post("/api/tickets/{id}/ai/classification", ticket.getId())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedCategory").value("BILLING"));

        mockMvc.perform(post("/api/tickets/{id}/ai/classification", ticket.getId())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedCategory").value("BILLING"));
    }

    @Test
    void customerCannotRequestClassification() throws Exception {
        Organization org = createOrganization("Gamma");
        User customer = createUser(org, Role.CUSTOMER, "customer@gamma.com", "Customer");
        Ticket ticket = createTicket(org, customer, "Feature request", "Please add dashboard export");

        mockMvc.perform(post("/api/tickets/{id}/ai/classification", ticket.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotRequestClassificationForAnotherOrganizationTicket() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");
        User agentA = createUser(orgA, Role.AGENT, "agent@orga.com", "Agent A");
        Ticket ticketB = createTicket(orgB, customerB, "OrgB issue", "Timeout and crash in checkout");

        mockMvc.perform(post("/api/tickets/{id}/ai/classification", ticketB.getId())
                        .header("Authorization", bearerToken(agentA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aiClassificationDoesNotChangeTicketCategoryOrPriority() throws Exception {
        Organization org = createOrganization("Delta");
        User customer = createUser(org, Role.CUSTOMER, "customer@delta.com", "Customer");
        User manager = createUser(org, Role.MANAGER, "manager@delta.com", "Manager");
        Ticket ticket = createTicket(org, customer, "Security concern", "Potential security vulnerability found");

        mockMvc.perform(post("/api/tickets/{id}/ai/classification", ticket.getId())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedCategory").value("SECURITY"))
                .andExpect(jsonPath("$.suggestedPriority").value("URGENT"));

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId())
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACCOUNT"))
                .andExpect(jsonPath("$.priority").value("LOW"));
    }

    @Test
    void existingTicketCreationStillWorksWhenAiClassificationIsNotCalled() throws Exception {
        Organization org = createOrganization("Epsilon");
        User customer = createUser(org, Role.CUSTOMER, "customer@epsilon.com", "Customer");

        CreateTicketRequest request = new CreateTicketRequest(
                "Cannot access account",
                "User cannot login after reset",
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL
        );

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("TECHNICAL"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
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

    private Ticket createTicket(Organization organization, User requester, String title, String description) {
        Ticket ticket = new Ticket();
        ticket.setOrganization(organization);
        ticket.setRequester(requester);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.LOW);
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
