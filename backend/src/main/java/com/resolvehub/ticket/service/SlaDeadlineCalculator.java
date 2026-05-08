package com.resolvehub.ticket.service;

import com.resolvehub.ticket.domain.TicketPriority;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class SlaDeadlineCalculator {

    public OffsetDateTime calculateDueAt(OffsetDateTime createdAt, TicketPriority priority) {
        return switch (priority) {
            case URGENT -> createdAt.plusHours(4);
            case HIGH -> createdAt.plusHours(8);
            case MEDIUM -> createdAt.plusHours(24);
            case LOW -> createdAt.plusHours(72);
        };
    }
}
