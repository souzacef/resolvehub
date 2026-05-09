package com.resolvehub.seed;

import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticket.service.SlaDeadlineCalculator;
import com.resolvehub.ticketcomment.domain.TicketComment;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DevelopmentDemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_ORG_NAME = "ResolveHub Demo Org";
    private static final String DEMO_PASSWORD = "Password123!";

    private final Environment environment;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlaDeadlineCalculator slaDeadlineCalculator;

    @Value("${resolvehub.seed.demo-enabled:false}")
    private boolean demoEnabled;

    public DevelopmentDemoDataSeeder(
            Environment environment,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository,
            TicketCommentRepository ticketCommentRepository,
            PasswordEncoder passwordEncoder,
            SlaDeadlineCalculator slaDeadlineCalculator
    ) {
        this.environment = environment;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.passwordEncoder = passwordEncoder;
        this.slaDeadlineCalculator = slaDeadlineCalculator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!shouldSeed()) {
            return;
        }

        Organization organization = ensureOrganization();

        ensureUser(organization, "Demo Admin", "admin@resolvehub.dev", Role.ADMIN);
        User manager = ensureUser(organization, "Demo Manager", "manager@resolvehub.dev", Role.MANAGER);
        User agent = ensureUser(organization, "Demo Agent", "agent@resolvehub.dev", Role.AGENT);
        User customer = ensureUser(organization, "Demo Customer", "customer@resolvehub.dev", Role.CUSTOMER);

        Ticket incident = ensureTicket(
                organization,
                customer,
                agent,
                "Production login outage",
                "Multiple customers report failed login attempts after this morning deployment.",
                TicketStatus.IN_PROGRESS,
                TicketPriority.URGENT,
                TicketCategory.TECHNICAL,
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(3)
        );

        Ticket billing = ensureTicket(
                organization,
                customer,
                manager,
                "Duplicate charge on monthly invoice",
                "Customer was charged twice for the same monthly subscription cycle.",
                TicketStatus.WAITING_CUSTOMER,
                TicketPriority.HIGH,
                TicketCategory.BILLING,
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(18)
        );

        Ticket feature = ensureTicket(
                organization,
                customer,
                null,
                "CSV export for ticket list",
                "Customer requests CSV export for ticket search results.",
                TicketStatus.RESOLVED,
                TicketPriority.LOW,
                TicketCategory.FEATURE_REQUEST,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2)
        );

        Ticket account = ensureTicket(
                organization,
                customer,
                null,
                "Unable to update profile email",
                "Profile page returns validation error when changing the account email.",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM,
                TicketCategory.ACCOUNT,
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(6)
        );

        ensureComment(incident, customer, "Issue started around 09:15 UTC for several users.", false);
        ensureComment(incident, agent, "Investigating auth service logs and rollback options.", true);

        ensureComment(billing, customer, "Attached payment receipts for both charges.", false);
        ensureComment(billing, manager, "Requested finance confirmation from payment gateway.", true);

        ensureComment(feature, customer, "This would help with monthly reporting.", false);
        ensureComment(feature, agent, "Feature request accepted and linked to roadmap backlog.", false);

        ensureComment(account, customer, "Error appears only when setting a corporate domain email.", false);
    }

    private boolean shouldSeed() {
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        return isDevProfile || demoEnabled;
    }

    private Organization ensureOrganization() {
        return organizationRepository.findByName(DEMO_ORG_NAME)
                .orElseGet(() -> {
                    Organization organization = new Organization();
                    organization.setName(DEMO_ORG_NAME);
                    organization.setStatus("ACTIVE");
                    return organizationRepository.save(organization);
                });
    }

    private User ensureUser(Organization organization, String name, String email, Role role) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        boolean isNewUser = user.getId() == null;
        boolean changed = isNewUser;

        if (isNewUser) {
            user.setEmail(email);
        }

        if (user.getOrganization() == null
                || !organization.getId().equals(user.getOrganization().getId())) {
            user.setOrganization(organization);
            changed = true;
        }

        if (!Objects.equals(user.getName(), name)) {
            user.setName(name);
            changed = true;
        }

        if (user.getRole() != role) {
            user.setRole(role);
            changed = true;
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
            changed = true;
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(DEMO_PASSWORD, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            changed = true;
        }

        if (changed) {
            return userRepository.save(user);
        }

        return user;
    }

    private Ticket ensureTicket(
            Organization organization,
            User requester,
            User assignee,
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            OffsetDateTime createdAt
    ) {
        Ticket ticket = ticketRepository.findByOrganizationIdAndTitle(organization.getId(), title)
                .orElseGet(Ticket::new);

        boolean isNewTicket = ticket.getId() == null;
        boolean changed = isNewTicket;

        if (isNewTicket) {
            ticket.setCreatedAt(createdAt);
            ticket.setTitle(title);
        }

        if (ticket.getOrganization() == null
                || !organization.getId().equals(ticket.getOrganization().getId())) {
            ticket.setOrganization(organization);
            changed = true;
        }

        if (ticket.getRequester() == null
                || !requester.getId().equals(ticket.getRequester().getId())) {
            ticket.setRequester(requester);
            changed = true;
        }

        if (!sameUser(ticket.getAssignee(), assignee)) {
            ticket.setAssignee(assignee);
            changed = true;
        }

        if (!Objects.equals(ticket.getDescription(), description)) {
            ticket.setDescription(description);
            changed = true;
        }

        if (ticket.getStatus() != status) {
            ticket.setStatus(status);
            changed = true;
        }

        if (ticket.getPriority() != priority) {
            ticket.setPriority(priority);
            changed = true;
        }

        if (ticket.getCategory() != category) {
            ticket.setCategory(category);
            changed = true;
        }

        OffsetDateTime expectedDueAt = slaDeadlineCalculator.calculateDueAt(
                ticket.getCreatedAt() == null ? createdAt : ticket.getCreatedAt(),
                priority
        );
        if (!Objects.equals(ticket.getSlaDueAt(), expectedDueAt)) {
            ticket.setSlaDueAt(expectedDueAt);
            changed = true;
        }

        if (changed) {
            return ticketRepository.save(ticket);
        }

        return ticket;
    }

    private void ensureComment(Ticket ticket, User author, String body, boolean internal) {
        if (ticketCommentRepository.existsByTicketIdAndBody(ticket.getId(), body)) {
            return;
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody(body);
        comment.setInternal(internal);
        ticketCommentRepository.save(comment);
    }

    private boolean sameUser(User left, User right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getId(), right.getId());
    }
}
