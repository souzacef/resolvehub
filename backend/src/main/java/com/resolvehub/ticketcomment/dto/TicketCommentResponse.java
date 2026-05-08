package com.resolvehub.ticketcomment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketCommentResponse(
        UUID id,
        UUID ticketId,
        UUID authorId,
        String body,
        boolean internal,
        OffsetDateTime createdAt
) {
}
