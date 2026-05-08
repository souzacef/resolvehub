package com.resolvehub.audit.dto;

import com.resolvehub.audit.domain.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getOrganization().getId(),
                auditLog.getActor() == null ? null : auditLog.getActor().getId(),
                auditLog.getTicket() == null ? null : auditLog.getTicket().getId(),
                auditLog.getAction(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
