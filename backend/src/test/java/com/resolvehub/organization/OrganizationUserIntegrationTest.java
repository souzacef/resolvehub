package com.resolvehub.organization;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resolvehub.audit.repository.AuditLogRepository;
import com.resolvehub.common.security.JwtService;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
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
class OrganizationUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void adminCanListOrganizationUsers() throws Exception {
        Organization organization = createOrganization("Acme");
        User admin = createUser(organization, Role.ADMIN, "admin@acme.com", "Admin");
        User manager = createUser(organization, Role.MANAGER, "manager@acme.com", "Manager");
        User customer = createUser(organization, Role.CUSTOMER, "customer@acme.com", "Customer");

        mockMvc.perform(get("/api/organization/users")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", containsInAnyOrder(
                        admin.getEmail(),
                        manager.getEmail(),
                        customer.getEmail()
                )))
                .andExpect(jsonPath("$[*].role", hasItem("ADMIN")));
    }

    @Test
    void managerCanListOrganizationUsers() throws Exception {
        Organization organization = createOrganization("Beta");
        User manager = createUser(organization, Role.MANAGER, "manager@beta.com", "Manager");
        User agent = createUser(organization, Role.AGENT, "agent@beta.com", "Agent");

        mockMvc.perform(get("/api/organization/users")
                        .header("Authorization", bearerToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", containsInAnyOrder(
                        manager.getEmail(),
                        agent.getEmail()
                )));
    }

    @Test
    void agentCanListOrganizationUsers() throws Exception {
        Organization organization = createOrganization("Gamma");
        User manager = createUser(organization, Role.MANAGER, "manager@gamma.com", "Manager");
        User agent = createUser(organization, Role.AGENT, "agent@gamma.com", "Agent");

        mockMvc.perform(get("/api/organization/users")
                        .header("Authorization", bearerToken(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", containsInAnyOrder(
                        manager.getEmail(),
                        agent.getEmail()
                )));
    }

    @Test
    void customerCannotListOrganizationUsers() throws Exception {
        Organization organization = createOrganization("Delta");
        User customer = createUser(organization, Role.CUSTOMER, "customer@delta.com", "Customer");

        mockMvc.perform(get("/api/organization/users")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersFromAnotherOrganizationAreNotReturned() throws Exception {
        Organization orgA = createOrganization("OrgA");
        Organization orgB = createOrganization("OrgB");

        User adminA = createUser(orgA, Role.ADMIN, "admin@orga.com", "Admin A");
        User managerA = createUser(orgA, Role.MANAGER, "manager@orga.com", "Manager A");
        User agentB = createUser(orgB, Role.AGENT, "agent@orgb.com", "Agent B");

        mockMvc.perform(get("/api/organization/users")
                        .header("Authorization", bearerToken(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", containsInAnyOrder(
                        adminA.getEmail(),
                        managerA.getEmail()
                )))
                .andExpect(jsonPath("$[*].email", not(hasItem(agentB.getEmail()))));
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
