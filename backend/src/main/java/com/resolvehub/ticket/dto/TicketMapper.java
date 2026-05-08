package com.resolvehub.ticket.dto;

import com.resolvehub.ticket.domain.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getOrganization().getId(),
                ticket.getRequester().getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
