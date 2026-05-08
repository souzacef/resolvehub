package com.resolvehub.ticket;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resolvehub.common.security.JwtService;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketOverdueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void ticketWithPastSlaDueAtAndOpenStatusIsOverdue() throws Exception {
        Organization org = createOrganization("Acme");
        User customer = createUser(org, Role.CUSTOMER, "customer@acme.com", "Customer");
        Ticket overdueTicket = createTicket(org, customer, TicketStatus.OPEN, OffsetDateTime.now().minusHours(1), "Overdue open");

        mockMvc.perform(get("/api/tickets/{id}", overdueTicket.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue").value(true));
    }

    @Test
    void ticketWithFutureSlaDueAtIsNotOverdue() throws Exception {
        Organization org = createOrganization("Beta");
        User customer = createUser(org, Role.CUSTOMER, "customer@beta.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.OPEN, OffsetDateTime.now().plusHours(2), "Future SLA");

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void resolvedTicketWithPastSlaDueAtIsNotOverdue() throws Exception {
        Organization org = createOrganization("Gamma");
        User customer = createUser(org, Role.CUSTOMER, "customer@gamma.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.RESOLVED, OffsetDateTime.now().minusHours(4), "Resolved ticket");

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void closedTicketWithPastSlaDueAtIsNotOverdue() throws Exception {
        Organization org = createOrganization("Delta");
        User customer = createUser(org, Role.CUSTOMER, "customer@delta.com", "Customer");
        Ticket ticket = createTicket(org, customer, TicketStatus.CLOSED, OffsetDateTime.now().minusHours(4), "Closed ticket");

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void customerOnlySeesOverdueTicketsFromOwnTickets() throws Exception {
        Organization org = createOrganization("Epsilon");
        User customerA = createUser(org, Role.CUSTOMER, "a@epsilon.com", "Customer A");
        User customerB = createUser(org, Role.CUSTOMER, "b@epsilon.com", "Customer B");

        Ticket customerAOverdue = createTicket(org, customerA, TicketStatus.OPEN, OffsetDateTime.now().minusHours(3), "A overdue");
        createTicket(org, customerB, TicketStatus.OPEN, OffsetDateTime.now().minusHours(3), "B overdue");

        mockMvc.perform(get("/api/tickets")
                        .param("overdue", "true")
                        .header("Authorization", bearerToken(customerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(customerAOverdue.getId().toString()))
                .andExpect(jsonPath("$[0].requesterId").value(customerA.getId().toString()))
                .andExpect(jsonPath("$[0].overdue").value(true));
    }

    @Test
    void managerAndAdminCanFilterOverdueTicketsInTheirOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");

        User customerA = createUser(orgA, Role.CUSTOMER, "customer@orga.com", "Customer A");
        User managerA = createUser(orgA, Role.MANAGER, "manager@orga.com", "Manager A");
        User adminA = createUser(orgA, Role.ADMIN, "admin@orga.com", "Admin A");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgb.com", "Customer B");

        Ticket overdueOrgA = createTicket(orgA, customerA, TicketStatus.OPEN, OffsetDateTime.now().minusHours(2), "OrgA overdue");
        createTicket(orgA, customerA, TicketStatus.OPEN, OffsetDateTime.now().plusHours(6), "OrgA future");
        createTicket(orgB, customerB, TicketStatus.OPEN, OffsetDateTime.now().minusHours(2), "OrgB overdue");

        mockMvc.perform(get("/api/tickets")
                        .param("overdue", "true")
                        .header("Authorization", bearerToken(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(overdueOrgA.getId().toString()))
                .andExpect(jsonPath("$[0].overdue").value(true));

        mockMvc.perform(get("/api/tickets")
                        .param("overdue", "true")
                        .header("Authorization", bearerToken(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(overdueOrgA.getId().toString()))
                .andExpect(jsonPath("$[0].overdue").value(true));
    }

    @Test
    void usersCannotSeeOverdueTicketsFromAnotherOrganization() throws Exception {
        Organization orgA = createOrganization("OrgX");
        Organization orgB = createOrganization("OrgY");

        User managerA = createUser(orgA, Role.MANAGER, "manager@orgx.com", "Manager A");
        User customerB = createUser(orgB, Role.CUSTOMER, "customer@orgy.com", "Customer B");

        createTicket(orgB, customerB, TicketStatus.OPEN, OffsetDateTime.now().minusHours(2), "OrgY overdue");

        mockMvc.perform(get("/api/tickets")
                        .param("overdue", "true")
                        .header("Authorization", bearerToken(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
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

    private Ticket createTicket(
            Organization organization,
            User requester,
            TicketStatus status,
            OffsetDateTime slaDueAt,
            String title
    ) {
        Ticket ticket = new Ticket();
        ticket.setOrganization(organization);
        ticket.setRequester(requester);
        ticket.setTitle(title);
        ticket.setDescription("Description for " + title);
        ticket.setStatus(status);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCategory(TicketCategory.OTHER);
        ticket.setSlaDueAt(slaDueAt);
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
