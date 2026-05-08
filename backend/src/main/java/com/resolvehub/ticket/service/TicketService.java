package com.resolvehub.ticket.service;

import com.resolvehub.ai.dto.TicketClassificationSuggestion;
import com.resolvehub.ai.service.TicketAiClassifier;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.audit.dto.AuditLogResponse;
import com.resolvehub.audit.service.AuditLogService;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.dto.CreateTicketRequest;
import com.resolvehub.ticket.dto.TicketMapper;
import com.resolvehub.ticket.dto.TicketResponse;
import com.resolvehub.ticket.dto.UpdateTicketAssigneeRequest;
import com.resolvehub.ticket.dto.UpdateTicketStatusRequest;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
            TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.WAITING_CUSTOMER, TicketStatus.RESOLVED, TicketStatus.CLOSED),
            TicketStatus.WAITING_CUSTOMER, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS),
            TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class)
    );

    private static final Map<TicketStatus, Set<TicketStatus>> CUSTOMER_ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.CLOSED),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.IN_PROGRESS)
    );
    private static final Set<TicketStatus> NON_OVERDUE_STATUSES = EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;
    private final SlaDeadlineCalculator slaDeadlineCalculator;
    private final AuditLogService auditLogService;
    private final TicketAiClassifier ticketAiClassifier;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketMapper ticketMapper,
            SlaDeadlineCalculator slaDeadlineCalculator,
            AuditLogService auditLogService,
            TicketAiClassifier ticketAiClassifier
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketMapper = ticketMapper;
        this.slaDeadlineCalculator = slaDeadlineCalculator;
        this.auditLogService = auditLogService;
        this.ticketAiClassifier = ticketAiClassifier;
    }

    @Transactional
    public TicketResponse createTicket(ResolveHubUserPrincipal principal, CreateTicketRequest request) {
        requirePrincipal(principal);

        if (principal.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can create tickets");
        }

        User requester = userRepository.findByIdAndOrganizationId(principal.getUserId(), principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Requester not found in organization"));

        Ticket ticket = new Ticket();
        ticket.setOrganization(requester.getOrganization());
        ticket.setRequester(requester);
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority());
        ticket.setCategory(request.category());
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        ticket.setCreatedAt(createdAt);
        ticket.setSlaDueAt(slaDeadlineCalculator.calculateDueAt(createdAt, request.priority()));
        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.logTicketCreated(principal, savedTicket);
        return ticketMapper.toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(ResolveHubUserPrincipal principal, Boolean overdue) {
        requirePrincipal(principal);
        boolean overdueOnly = Boolean.TRUE.equals(overdue);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<Ticket> tickets;
        if (principal.getRole() == Role.CUSTOMER) {
            if (overdueOnly) {
                tickets = ticketRepository.findByOrganizationIdAndRequesterIdAndStatusNotInAndSlaDueAtBeforeOrderByCreatedAtDesc(
                        principal.getOrganizationId(),
                        principal.getUserId(),
                        NON_OVERDUE_STATUSES,
                        now
                );
            } else {
                tickets = ticketRepository.findByOrganizationIdAndRequesterIdOrderByCreatedAtDesc(
                        principal.getOrganizationId(),
                        principal.getUserId()
                );
            }
        } else if (principal.getRole() == Role.AGENT
                || principal.getRole() == Role.MANAGER
                || principal.getRole() == Role.ADMIN) {
            if (overdueOnly) {
                tickets = ticketRepository.findByOrganizationIdAndStatusNotInAndSlaDueAtBeforeOrderByCreatedAtDesc(
                        principal.getOrganizationId(),
                        NON_OVERDUE_STATUSES,
                        now
                );
            } else {
                tickets = ticketRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId());
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to list tickets");
        }

        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(ResolveHubUserPrincipal principal, UUID ticketId) {
        requirePrincipal(principal);

        Ticket ticket;
        if (principal.getRole() == Role.CUSTOMER) {
            ticket = ticketRepository
                    .findByIdAndOrganizationIdAndRequesterId(
                            ticketId,
                            principal.getOrganizationId(),
                            principal.getUserId()
                    )
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        } else if (principal.getRole() == Role.AGENT
                || principal.getRole() == Role.MANAGER
                || principal.getRole() == Role.ADMIN) {
            ticket = ticketRepository
                    .findByIdAndOrganizationId(ticketId, principal.getOrganizationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to view tickets");
        }

        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicketStatus(
            ResolveHubUserPrincipal principal,
            UUID ticketId,
            UpdateTicketStatusRequest request
    ) {
        requirePrincipal(principal);

        Ticket ticket = ticketRepository.findByIdAndOrganizationId(ticketId, principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus targetStatus = request.status();

        validateTransition(currentStatus, targetStatus);
        validateRolePermissionForStatusChange(principal, ticket, currentStatus, targetStatus);

        ticket.setStatus(targetStatus);
        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.logTicketStatusChanged(principal, savedTicket, currentStatus.name(), targetStatus.name());
        return ticketMapper.toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse updateTicketAssignee(
            ResolveHubUserPrincipal principal,
            UUID ticketId,
            UpdateTicketAssigneeRequest request
    ) {
        requirePrincipal(principal);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        Ticket ticket = ticketRepository.findByIdAndOrganizationId(ticketId, principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed tickets cannot be assigned");
        }

        Role role = principal.getRole();
        if (role == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot assign tickets");
        }

        if (role == Role.AGENT) {
            assignAsAgent(principal, ticket, request);
            Ticket savedTicket = ticketRepository.save(ticket);
            auditLogService.logTicketAssigned(principal, savedTicket, request.assigneeId());
            return ticketMapper.toResponse(savedTicket);
        }

        if (role == Role.MANAGER || role == Role.ADMIN) {
            UUID previousAssigneeId = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
            assignAsManagerOrAdmin(principal, ticket, request);
            Ticket savedTicket = ticketRepository.save(ticket);

            if (request.assigneeId() == null) {
                auditLogService.logTicketUnassigned(principal, savedTicket, previousAssigneeId);
            } else {
                auditLogService.logTicketAssigned(principal, savedTicket, request.assigneeId());
            }
            return ticketMapper.toResponse(savedTicket);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to assign tickets");
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getTicketAuditLogs(ResolveHubUserPrincipal principal, UUID ticketId) {
        return auditLogService.getTicketAuditLogs(principal, ticketId);
    }

    @Transactional(readOnly = true)
    public TicketClassificationSuggestion requestTicketClassification(ResolveHubUserPrincipal principal, UUID ticketId) {
        requirePrincipal(principal);

        Role role = principal.getRole();
        if (role == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot request AI classification");
        }
        if (role != Role.AGENT && role != Role.MANAGER && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to request AI classification");
        }

        Ticket ticket = ticketRepository.findByIdAndOrganizationId(ticketId, principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        return ticketAiClassifier.classify(ticket);
    }

    private void assignAsAgent(
            ResolveHubUserPrincipal principal,
            Ticket ticket,
            UpdateTicketAssigneeRequest request
    ) {
        UUID assigneeId = request.assigneeId();

        if (assigneeId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agents cannot unassign tickets");
        }

        if (ticket.getAssignee() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agents can only assign unassigned tickets");
        }

        if (!principal.getUserId().equals(assigneeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agents can only assign tickets to themselves");
        }

        User assignee = findAssignableUser(principal.getOrganizationId(), assigneeId);
        ticket.setAssignee(assignee);
    }

    private void assignAsManagerOrAdmin(
            ResolveHubUserPrincipal principal,
            Ticket ticket,
            UpdateTicketAssigneeRequest request
    ) {
        UUID assigneeId = request.assigneeId();
        if (assigneeId == null) {
            ticket.setAssignee(null);
            return;
        }

        User assignee = findAssignableUser(principal.getOrganizationId(), assigneeId);
        ticket.setAssignee(assignee);
    }

    private User findAssignableUser(UUID organizationId, UUID assigneeId) {
        User assignee = userRepository.findByIdAndOrganizationId(assigneeId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found in organization"));

        if (assignee.getRole() == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customers cannot be assigned to tickets");
        }

        return assignee;
    }

    private void validateTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        Set<TicketStatus> nextStatuses = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!nextStatuses.contains(targetStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + currentStatus + " to " + targetStatus
            );
        }
    }

    private void validateRolePermissionForStatusChange(
            ResolveHubUserPrincipal principal,
            Ticket ticket,
            TicketStatus currentStatus,
            TicketStatus targetStatus
    ) {
        Role role = principal.getRole();

        if (role == Role.CUSTOMER) {
            if (!ticket.getRequester().getId().equals(principal.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only update their own tickets");
            }

            Set<TicketStatus> customerTargets = CUSTOMER_ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
            if (!customerTargets.contains(targetStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Customers are not allowed to change ticket status from " + currentStatus + " to " + targetStatus
                );
            }
            return;
        }

        if (role == Role.AGENT || role == Role.MANAGER || role == Role.ADMIN) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to update ticket status");
    }

    private void requirePrincipal(ResolveHubUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
