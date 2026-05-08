package com.resolvehub.ai.service;

import com.resolvehub.ai.dto.TicketClassificationSuggestion;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "resolvehub.ai", name = "provider", havingValue = "fake", matchIfMissing = true)
public class FakeTicketAiClassifier implements TicketAiClassifier {

    @Override
    public TicketClassificationSuggestion classify(Ticket ticket) {
        String text = (ticket.getTitle() + " " + ticket.getDescription()).toLowerCase(Locale.ROOT);

        // Rule order is intentional and deterministic: first matching rule wins.
        if (containsAny(text, "security", "breach", "vulnerability", "malware", "phishing", "unauthorized")) {
            return new TicketClassificationSuggestion(
                    TicketCategory.SECURITY,
                    TicketPriority.URGENT,
                    "Detected security-risk keywords in the ticket description."
            );
        }

        if (containsAny(text, "invoice", "billing", "refund", "payment", "charge", "charged", "subscription")) {
            return new TicketClassificationSuggestion(
                    TicketCategory.BILLING,
                    TicketPriority.HIGH,
                    "Detected billing and payment-related keywords."
            );
        }

        if (containsAny(text, "feature", "enhancement", "request", "roadmap", "improvement")) {
            return new TicketClassificationSuggestion(
                    TicketCategory.FEATURE_REQUEST,
                    TicketPriority.LOW,
                    "Detected product enhancement request language."
            );
        }

        if (containsAny(text, "account", "password", "profile", "username", "locked")) {
            return new TicketClassificationSuggestion(
                    TicketCategory.ACCOUNT,
                    TicketPriority.MEDIUM,
                    "Detected account-access and identity-management keywords."
            );
        }

        if (containsAny(text, "login", "error", "exception", "timeout", "crash", "failed", "cannot")) {
            return new TicketClassificationSuggestion(
                    TicketCategory.TECHNICAL,
                    TicketPriority.HIGH,
                    "Detected technical incident and reliability keywords."
            );
        }

        return new TicketClassificationSuggestion(
                TicketCategory.OTHER,
                TicketPriority.MEDIUM,
                "No dominant pattern detected; routed to general support triage."
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
