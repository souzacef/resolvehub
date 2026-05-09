package com.resolvehub.ticketcomment.repository;

import com.resolvehub.ticketcomment.domain.TicketComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<TicketComment> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(UUID ticketId);

    boolean existsByTicketIdAndBody(UUID ticketId, String body);
}
