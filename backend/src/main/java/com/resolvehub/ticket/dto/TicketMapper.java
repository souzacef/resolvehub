package com.resolvehub.ticket.dto;

import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getOrganization().getId(),
                ticket.getRequester().getId(),
                ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getSlaDueAt(),
                isOverdue(ticket),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private boolean isOverdue(Ticket ticket) {
        return ticket.getSlaDueAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))
                && ticket.getStatus() != TicketStatus.RESOLVED
                && ticket.getStatus() != TicketStatus.CLOSED;
    }
}
