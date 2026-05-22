package com.resolvehub.ticket;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.dto.CreateTicketRequest;
import com.resolvehub.ticket.dto.TicketResponse;
import com.resolvehub.ticket.dto.UpdateTicketAssigneeRequest;
import com.resolvehub.ticket.dto.UpdateTicketStatusRequest;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.time.Duration;
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
class TicketSlaIntegrationTest {

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
    void creatingUrgentTicketSetsSlaAboutFourHoursAfterCreatedAt() throws Exception {
        TicketResponse response = createTicketWithPriority(TicketPriority.URGENT);
        assertSlaOffsetHours(response.createdAt(), response.slaDueAt(), 4);
    }

    @Test
    void creatingHighTicketSetsSlaAboutEightHoursAfterCreatedAt() throws Exception {
        TicketResponse response = createTicketWithPriority(TicketPriority.HIGH);
        assertSlaOffsetHours(response.createdAt(), response.slaDueAt(), 8);
    }

    @Test
    void creatingMediumTicketSetsSlaAboutTwentyFourHoursAfterCreatedAt() throws Exception {
        TicketResponse response = createTicketWithPriority(TicketPriority.MEDIUM);
        assertSlaOffsetHours(response.createdAt(), response.slaDueAt(), 24);
    }

    @Test
    void creatingLowTicketSetsSlaAboutSeventyTwoHoursAfterCreatedAt() throws Exception {
        TicketResponse response = createTicketWithPriority(TicketPriority.LOW);
        assertSlaOffsetHours(response.createdAt(), response.slaDueAt(), 72);
    }

    @Test
    void ticketResponseIncludesSlaDueAt() throws Exception {
        TicketResponse response = createTicketWithPriority(TicketPriority.HIGH);
        assertTrue(response.slaDueAt() != null);
    }

    @Test
    void changingStatusDoesNotRecalculateSlaDueAt() throws Exception {
        Organization organization = createOrganization("StatusOrg");
        User customer = createUser(organization, Role.CUSTOMER, "customer@status.org", "Customer");
        User agent = createUser(organization, Role.AGENT, "agent@status.org", "Agent");

        TicketResponse created = createTicket(organization, customer, TicketPriority.HIGH, "Status SLA");
        assignTicketToUser(created.id(), agent);

        MvcResult result = mockMvc.perform(patch("/api/tickets/{id}/status", created.id())
                        .header("Authorization", bearerToken(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketStatusRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andReturn();

        TicketResponse updated = objectMapper.readValue(result.getResponse().getContentAsString(), TicketResponse.class);
        assertSameInstantWithinTolerance(created.slaDueAt(), updated.slaDueAt(), Duration.ofSeconds(1));
    }

    @Test
    void assignmentDoesNotRecalculateSlaDueAt() throws Exception {
        Organization organization = createOrganization("AssignOrg");
        User customer = createUser(organization, Role.CUSTOMER, "customer@assign.org", "Customer");
        User manager = createUser(organization, Role.MANAGER, "manager@assign.org", "Manager");
        User agent = createUser(organization, Role.AGENT, "agent@assign.org", "Agent");

        TicketResponse created = createTicket(organization, customer, TicketPriority.MEDIUM, "Assignment SLA");

        MvcResult result = mockMvc.perform(patch("/api/tickets/{id}/assignee", created.id())
                        .header("Authorization", bearerToken(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTicketAssigneeRequest(agent.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(agent.getId().toString()))
                .andReturn();

        TicketResponse updated = objectMapper.readValue(result.getResponse().getContentAsString(), TicketResponse.class);
        assertSameInstantWithinTolerance(created.slaDueAt(), updated.slaDueAt(), Duration.ofSeconds(1));
    }

    private void assignTicketToUser(java.util.UUID ticketId, User assignee) {
        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalStateException("Ticket not found for assignment in test"));
        ticket.setAssignee(assignee);
        ticketRepository.save(ticket);
    }

    private TicketResponse createTicketWithPriority(TicketPriority priority) throws Exception {
        Organization organization = createOrganization("Org-" + priority.name());
        User customer = createUser(organization, Role.CUSTOMER, "customer+" + priority.name().toLowerCase() + "@example.com", "Customer");
        return createTicket(organization, customer, priority, "Priority " + priority.name());
    }

    private TicketResponse createTicket(
            Organization organization,
            User customer,
            TicketPriority priority,
            String title
    ) throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                title,
                "Ticket description",
                priority,
                TicketCategory.TECHNICAL,
                null
        );

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TicketResponse.class);
    }

    private void assertSlaOffsetHours(OffsetDateTime createdAt, OffsetDateTime slaDueAt, long expectedHours) {
        Duration delta = Duration.between(createdAt.plusHours(expectedHours), slaDueAt).abs();
        assertTrue(delta.toSeconds() <= 5, "SLA offset should be close to " + expectedHours + " hours");
    }

    private void assertSameInstantWithinTolerance(
            OffsetDateTime expected,
            OffsetDateTime actual,
            Duration tolerance
    ) {
        Duration delta = Duration.between(expected, actual).abs();
        assertTrue(
                delta.compareTo(tolerance) <= 0,
                "Timestamps should match within " + tolerance + ". Expected=" + expected + ", actual=" + actual
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
