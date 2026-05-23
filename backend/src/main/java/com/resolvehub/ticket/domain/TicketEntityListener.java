package com.resolvehub.ticket.domain;

import com.resolvehub.common.spring.SpringContext;
import com.resolvehub.ticket.service.TicketNumberGenerator;
import jakarta.persistence.PrePersist;

public class TicketEntityListener {

    @PrePersist
    public void assignTicketNumber(Ticket ticket) {
        if (ticket.getTicketNumber() != null && !ticket.getTicketNumber().isBlank()) {
            return;
        }

        TicketNumberGenerator ticketNumberGenerator = SpringContext.getBean(TicketNumberGenerator.class);
        ticket.setTicketNumber(ticketNumberGenerator.nextTicketNumber());
    }
}
