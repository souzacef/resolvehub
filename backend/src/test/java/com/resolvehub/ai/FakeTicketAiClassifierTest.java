package com.resolvehub.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.resolvehub.ai.dto.TicketClassificationSuggestion;
import com.resolvehub.ai.service.FakeTicketAiClassifier;
import com.resolvehub.ticket.domain.Ticket;
import org.junit.jupiter.api.Test;

class FakeTicketAiClassifierTest {

    private final FakeTicketAiClassifier classifier = new FakeTicketAiClassifier();

    @Test
    void returnsDeterministicSuggestionForSameInput() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Login fails after password reset");
        ticket.setDescription("User gets error when trying to login from mobile app");

        TicketClassificationSuggestion first = classifier.classify(ticket);
        TicketClassificationSuggestion second = classifier.classify(ticket);

        assertEquals(first, second);
        assertEquals("ACCOUNT", first.suggestedCategory().name());
        assertEquals("MEDIUM", first.suggestedPriority().name());
    }
}
