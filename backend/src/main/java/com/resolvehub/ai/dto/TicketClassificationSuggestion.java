package com.resolvehub.ai.dto;

import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;

public record TicketClassificationSuggestion(
        TicketCategory suggestedCategory,
        TicketPriority suggestedPriority,
        String reasoning
) {
}
