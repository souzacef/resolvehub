package com.resolvehub.ticketcomment.service;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticketcomment.domain.TicketComment;
import com.resolvehub.ticketcomment.dto.CreateTicketCommentRequest;
import com.resolvehub.ticketcomment.dto.TicketCommentMapper;
import com.resolvehub.ticketcomment.dto.TicketCommentResponse;
import com.resolvehub.ticketcomment.repository.TicketCommentRepository;
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
public class TicketCommentService {

    private final TicketCommentRepository ticketCommentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketCommentMapper ticketCommentMapper;

    public TicketCommentService(
            TicketCommentRepository ticketCommentRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketCommentMapper ticketCommentMapper
    ) {
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketCommentMapper = ticketCommentMapper;
    }

    @Transactional
    public TicketCommentResponse addComment(
            ResolveHubUserPrincipal principal,
            UUID ticketId,
            CreateTicketCommentRequest request
    ) {
        requirePrincipal(principal);
        Ticket ticket = loadAuthorizedTicket(principal, ticketId);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed tickets cannot receive comments");
        }

        boolean internal = Boolean.TRUE.equals(request.internal());
        if (principal.getRole() == Role.CUSTOMER && internal) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot create internal comments");
        }

        User author = userRepository.findByIdAndOrganizationId(principal.getUserId(), principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Author not found in organization"));

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody(request.body().trim());
        comment.setInternal(internal);

        return ticketCommentMapper.toResponse(ticketCommentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(ResolveHubUserPrincipal principal, UUID ticketId) {
        requirePrincipal(principal);
        Ticket ticket = loadAuthorizedTicket(principal, ticketId);

        List<TicketComment> comments;
        if (principal.getRole() == Role.CUSTOMER) {
            comments = ticketCommentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticket.getId());
        } else {
            comments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        }

        return comments.stream().map(ticketCommentMapper::toResponse).toList();
    }

    private Ticket loadAuthorizedTicket(ResolveHubUserPrincipal principal, UUID ticketId) {
        Ticket ticket = ticketRepository.findByIdAndOrganizationId(ticketId, principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        Role role = principal.getRole();
        if (role == Role.CUSTOMER) {
            if (!ticket.getRequester().getId().equals(principal.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers can only access their own tickets");
            }
            return ticket;
        }

        if (role == Role.AGENT || role == Role.MANAGER || role == Role.ADMIN) {
            return ticket;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to access ticket comments");
    }

    private void requirePrincipal(ResolveHubUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
