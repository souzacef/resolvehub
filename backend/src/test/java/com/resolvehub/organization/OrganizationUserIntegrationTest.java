package com.resolvehub.organization;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.audit.repository.AuditLogRepository;
import com.resolvehub.common.security.JwtService;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.dto.CreateOrganizationUserRequest;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class OrganizationUserIntegrationTest {

    private static final String PASSWORD_POLICY_MESSAGE_SNIPPET =
            "uppercase, lowercase, number, special character, and no spaces";

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

    @Test
    void adminCanCreateCustomer() throws Exception {
        Organization organization = createOrganization("CreateCustomerOrg");
        User admin = createUser(organization, Role.ADMIN, "admin@createcustomer.org", "Admin");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Customer User",
                                "customer@createcustomer.org",
                                "StrongPass123!",
                                Role.CUSTOMER
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("customer@createcustomer.org"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        User created = userRepository.findByEmail("customer@createcustomer.org").orElseThrow();
        assertEquals(organization.getId(), created.getOrganization().getId());
        assertTrue(passwordEncoder.matches("StrongPass123!", created.getPasswordHash()));
    }

    @Test
    void adminCanCreateAgent() throws Exception {
        Organization organization = createOrganization("CreateAgentOrg");
        User admin = createUser(organization, Role.ADMIN, "admin@createagent.org", "Admin");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Agent User",
                                "agent@createagent.org",
                                "StrongPass123!",
                                Role.AGENT
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("agent@createagent.org"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void managerCanCreateCustomer() throws Exception {
        Organization organization = createOrganization("ManagerCustomerOrg");
        User manager = createUser(organization, Role.MANAGER, "manager@managercustomer.org", "Manager");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Customer User",
                                "customer@managercustomer.org",
                                "StrongPass123!",
                                Role.CUSTOMER
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void managerCanCreateAgent() throws Exception {
        Organization organization = createOrganization("ManagerAgentOrg");
        User manager = createUser(organization, Role.MANAGER, "manager@manageragent.org", "Manager");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Agent User",
                                "agent@manageragent.org",
                                "StrongPass123!",
                                Role.AGENT
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void managerCannotCreateAdmin() throws Exception {
        Organization organization = createOrganization("ManagerAdminOrg");
        User manager = createUser(organization, Role.MANAGER, "manager@manageradmin.org", "Manager");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Admin User",
                                "admin@manageradmin.org",
                                "StrongPass123!",
                                Role.ADMIN
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCannotCreateUsers() throws Exception {
        Organization organization = createOrganization("AgentNoCreateOrg");
        User agent = createUser(organization, Role.AGENT, "agent@nocreate.org", "Agent");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Customer User",
                                "customer@nocreate.org",
                                "StrongPass123!",
                                Role.CUSTOMER
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotCreateUsers() throws Exception {
        Organization organization = createOrganization("CustomerNoCreateOrg");
        User customer = createUser(organization, Role.CUSTOMER, "customer@nocreate.org", "Customer");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Agent User",
                                "agent@nocreate.org",
                                "StrongPass123!",
                                Role.AGENT
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        Organization organization = createOrganization("DuplicateOrg");
        User admin = createUser(organization, Role.ADMIN, "admin@duplicate.org", "Admin");

        createUser(organization, Role.CUSTOMER, "taken@duplicate.org", "Taken User");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "New User",
                                "taken@duplicate.org",
                                "StrongPass123!",
                                Role.CUSTOMER
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void createdUserBelongsToCurrentUsersOrganization() throws Exception {
        Organization orgA = createOrganization("OrgA");
        createOrganization("OrgB");

        User adminA = createUser(orgA, Role.ADMIN, "admin@orga.org", "Admin A");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Scoped User",
                                "scoped@orga.org",
                                "StrongPass123!",
                                Role.AGENT
                        ))))
                .andExpect(status().isCreated());

        User created = userRepository.findByEmail("scoped@orga.org").orElseThrow();
        assertEquals(orgA.getId(), created.getOrganization().getId());
    }

    @Test
    void organizationUserCreationAcceptsPasswordThatMatchesPolicy() throws Exception {
        Organization organization = createOrganization("PolicyOrg");
        User admin = createUser(organization, Role.ADMIN, "admin@policy-org.org", "Admin");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Policy User",
                                "policy.user@policy-org.org",
                                "Password123!",
                                Role.CUSTOMER
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("policy.user@policy-org.org"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        User created = userRepository.findByEmail("policy.user@policy-org.org").orElseThrow();
        assertTrue(passwordEncoder.matches("Password123!", created.getPasswordHash()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Teste de senha",
            "lowercaseonlypassword",
            "Password!",
            "Password123",
            "Aa1!"
    })
    void organizationUserCreationRejectsPasswordsThatDoNotMatchPolicy(String invalidPassword) throws Exception {
        Organization organization = createOrganization("WeakPasswordOrg");
        User admin = createUser(organization, Role.ADMIN, "admin@weak-password.org", "Admin");

        mockMvc.perform(post("/api/organization/users")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrganizationUserRequest(
                                "Weak Password User",
                                "weak.user@weak-password.org",
                                invalidPassword,
                                Role.CUSTOMER
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    assertTrue(
                            responseBody.contains(PASSWORD_POLICY_MESSAGE_SNIPPET),
                            "Expected password policy validation details in the response body"
                    );
                });
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
