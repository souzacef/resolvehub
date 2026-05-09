package com.resolvehub.ticket.repository;

import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Ticket> findByOrganizationIdAndRequesterIdOrderByCreatedAtDesc(UUID organizationId, UUID requesterId);

    List<Ticket> findByOrganizationIdAndStatusNotInAndSlaDueAtBeforeOrderByCreatedAtDesc(
            UUID organizationId,
            Collection<TicketStatus> statuses,
            OffsetDateTime now
    );

    List<Ticket> findByOrganizationIdAndRequesterIdAndStatusNotInAndSlaDueAtBeforeOrderByCreatedAtDesc(
            UUID organizationId,
            UUID requesterId,
            Collection<TicketStatus> statuses,
            OffsetDateTime now
    );

    Optional<Ticket> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Ticket> findByIdAndOrganizationIdAndRequesterId(UUID id, UUID organizationId, UUID requesterId);

    Optional<Ticket> findByOrganizationIdAndTitle(UUID organizationId, String title);
}
