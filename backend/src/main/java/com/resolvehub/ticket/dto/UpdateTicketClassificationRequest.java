package com.resolvehub.ticket.dto;

import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketClassificationRequest(
        @NotNull(message = "Category is required")
        TicketCategory category,
        @NotNull(message = "Priority is required")
        TicketPriority priority
) {
}
