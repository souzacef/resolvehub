package com.resolvehub.ai.service;

import com.resolvehub.ai.dto.TicketClassificationSuggestion;
import com.resolvehub.ticket.domain.Ticket;

public interface TicketAiClassifier {

    TicketClassificationSuggestion classify(Ticket ticket);
}
