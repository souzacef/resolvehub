package com.resolvehub.ticketcomment.controller;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.ticketcomment.dto.CreateTicketCommentRequest;
import com.resolvehub.ticketcomment.dto.TicketCommentResponse;
import com.resolvehub.ticketcomment.service.TicketCommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final TicketCommentService ticketCommentService;

    public TicketCommentController(TicketCommentService ticketCommentService) {
        this.ticketCommentService = ticketCommentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCommentResponse addComment(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateTicketCommentRequest request
    ) {
        return ticketCommentService.addComment(principal, ticketId, request);
    }

    @GetMapping
    public List<TicketCommentResponse> listComments(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @PathVariable UUID ticketId
    ) {
        return ticketCommentService.listComments(principal, ticketId);
    }
}
