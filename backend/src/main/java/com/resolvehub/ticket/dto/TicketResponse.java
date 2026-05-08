package com.resolvehub.ticket.dto;

import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID organizationId,
        UUID requesterId,
        UUID assigneeId,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
