package com.resolvehub.audit.repository;

import com.resolvehub.audit.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByTicketIdAndOrganizationIdOrderByCreatedAtAsc(UUID ticketId, UUID organizationId);
}
