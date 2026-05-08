package com.resolvehub.ticket.controller;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.audit.dto.AuditLogResponse;
import com.resolvehub.ticket.dto.CreateTicketRequest;
import com.resolvehub.ticket.dto.TicketResponse;
import com.resolvehub.ticket.dto.UpdateTicketAssigneeRequest;
import com.resolvehub.ticket.dto.UpdateTicketStatusRequest;
import com.resolvehub.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        return ticketService.createTicket(principal, request);
    }

    @GetMapping
    public List<TicketResponse> listTickets(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @RequestParam(required = false) Boolean overdue
    ) {
        return ticketService.listTickets(principal, overdue);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicketById(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ticketService.getTicketById(principal, id);
    }

    @GetMapping("/{id}/audit-logs")
    public List<AuditLogResponse> getTicketAuditLogs(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ticketService.getTicketAuditLogs(principal, id);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateTicketStatus(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ticketService.updateTicketStatus(principal, id, request);
    }

    @PatchMapping("/{id}/assignee")
    public TicketResponse updateTicketAssignee(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody UpdateTicketAssigneeRequest request
    ) {
        return ticketService.updateTicketAssignee(principal, id, request);
    }
}
