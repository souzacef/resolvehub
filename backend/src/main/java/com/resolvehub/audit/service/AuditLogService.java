package com.resolvehub.audit.service;

import com.resolvehub.audit.domain.AuditAction;
import com.resolvehub.audit.domain.AuditLog;
import com.resolvehub.audit.dto.AuditLogMapper;
import com.resolvehub.audit.dto.AuditLogResponse;
import com.resolvehub.audit.repository.AuditLogRepository;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            AuditLogMapper auditLogMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional
    public void logTicketCreated(ResolveHubUserPrincipal principal, Ticket ticket) {
        writeLog(principal, ticket, AuditAction.TICKET_CREATED,
                "Ticket created with priority=" + ticket.getPriority() + ", category=" + ticket.getCategory());
    }

    @Transactional
    public void logTicketStatusChanged(
            ResolveHubUserPrincipal principal,
            Ticket ticket,
            String oldStatus,
            String newStatus
    ) {
        writeLog(principal, ticket, AuditAction.TICKET_STATUS_CHANGED,
                "Status changed from " + oldStatus + " to " + newStatus);
    }

    @Transactional
    public void logTicketAssigned(ResolveHubUserPrincipal principal, Ticket ticket, UUID assigneeId) {
        writeLog(principal, ticket, AuditAction.TICKET_ASSIGNED,
                "Ticket assigned to userId=" + assigneeId);
    }

    @Transactional
    public void logTicketUnassigned(ResolveHubUserPrincipal principal, Ticket ticket, UUID previousAssigneeId) {
        writeLog(principal, ticket, AuditAction.TICKET_UNASSIGNED,
                "Ticket unassigned from userId=" + previousAssigneeId);
    }

    @Transactional
    public void logCommentAdded(ResolveHubUserPrincipal principal, Ticket ticket, boolean internal) {
        writeLog(principal, ticket, AuditAction.COMMENT_ADDED,
                "Comment added with internal=" + internal);
    }

    @Transactional
    public void logTicketClassificationUpdated(
            ResolveHubUserPrincipal principal,
            Ticket ticket,
            String oldCategory,
            String newCategory,
            String oldPriority,
            String newPriority
    ) {
        writeLog(principal, ticket, AuditAction.TICKET_CLASSIFICATION_UPDATED,
                "Classification changed from category=" + oldCategory + ", priority=" + oldPriority
                        + " to category=" + newCategory + ", priority=" + newPriority);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getTicketAuditLogs(ResolveHubUserPrincipal principal, UUID ticketId) {
        requirePrincipal(principal);

        Ticket ticket = ticketRepository.findByIdAndOrganizationId(ticketId, principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        validateTicketAuditVisibility(principal, ticket);

        return auditLogRepository.findByTicketIdAndOrganizationIdOrderByCreatedAtAsc(ticketId, principal.getOrganizationId())
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    private void writeLog(ResolveHubUserPrincipal principal, Ticket ticket, AuditAction action, String details) {
        requirePrincipal(principal);

        User actor = userRepository.findByIdAndOrganizationId(principal.getUserId(), principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Actor not found in organization"));

        AuditLog log = new AuditLog();
        log.setOrganization(ticket.getOrganization());
        log.setActor(actor);
        log.setTicket(ticket);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private void validateTicketAuditVisibility(ResolveHubUserPrincipal principal, Ticket ticket) {
        Role role = principal.getRole();

        if (role == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot view audit logs");
        }

        if (role == Role.MANAGER || role == Role.ADMIN) {
            return;
        }

        if (role == Role.AGENT) {
            if (ticket.getAssignee() == null || principal.getUserId().equals(ticket.getAssignee().getId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agents can only view audit logs for assigned or unassigned tickets");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to view audit logs");
    }

    private void requirePrincipal(ResolveHubUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
