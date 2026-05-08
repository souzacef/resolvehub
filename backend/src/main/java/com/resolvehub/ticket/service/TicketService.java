package com.resolvehub.ticket.service;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.dto.CreateTicketRequest;
import com.resolvehub.ticket.dto.TicketMapper;
import com.resolvehub.ticket.dto.TicketResponse;
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
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository, TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketMapper = ticketMapper;
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

        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(ResolveHubUserPrincipal principal) {
        requirePrincipal(principal);

        List<Ticket> tickets;
        if (principal.getRole() == Role.CUSTOMER) {
            tickets = ticketRepository.findByOrganizationIdAndRequesterIdOrderByCreatedAtDesc(
                    principal.getOrganizationId(),
                    principal.getUserId()
            );
        } else if (principal.getRole() == Role.AGENT
                || principal.getRole() == Role.MANAGER
                || principal.getRole() == Role.ADMIN) {
            tickets = ticketRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId());
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

    private void requirePrincipal(ResolveHubUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
