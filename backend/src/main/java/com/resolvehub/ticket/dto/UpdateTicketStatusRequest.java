package com.resolvehub.ticket.dto;

import com.resolvehub.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "status is required") TicketStatus status
) {
}
