package com.resolvehub.audit.dto;

import com.resolvehub.audit.domain.AuditAction;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID organizationId,
        UUID actorId,
        UUID ticketId,
        AuditAction action,
        String details,
        OffsetDateTime createdAt
) {
}
