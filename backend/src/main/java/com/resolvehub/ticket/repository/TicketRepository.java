package com.resolvehub.ticket.repository;

import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select t
            from Ticket t
            where t.organization.id = :organizationId
              and (t.assignee is null or t.assignee.id = :assigneeId)
            order by t.createdAt desc
            """)
    List<Ticket> findVisibleToAgentOrderByCreatedAtDesc(
            @Param("organizationId") UUID organizationId,
            @Param("assigneeId") UUID assigneeId
    );

    @Query("""
            select t
            from Ticket t
            where t.organization.id = :organizationId
              and t.status not in :statuses
              and t.slaDueAt < :now
              and (t.assignee is null or t.assignee.id = :assigneeId)
            order by t.createdAt desc
            """)
    List<Ticket> findOverdueVisibleToAgentOrderByCreatedAtDesc(
            @Param("organizationId") UUID organizationId,
            @Param("assigneeId") UUID assigneeId,
            @Param("statuses") Collection<TicketStatus> statuses,
            @Param("now") OffsetDateTime now
    );

    Optional<Ticket> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            select t
            from Ticket t
            where t.id = :id
              and t.organization.id = :organizationId
              and (t.assignee is null or t.assignee.id = :assigneeId)
            """)
    Optional<Ticket> findVisibleToAgentByIdAndOrganizationId(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId,
            @Param("assigneeId") UUID assigneeId
    );

    Optional<Ticket> findByIdAndOrganizationIdAndRequesterId(UUID id, UUID organizationId, UUID requesterId);

    Optional<Ticket> findByOrganizationIdAndTitle(UUID organizationId, String title);
}
