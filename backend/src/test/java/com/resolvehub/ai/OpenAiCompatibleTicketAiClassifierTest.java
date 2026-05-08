package com.resolvehub.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.ai.config.AiProviderProperties;
import com.resolvehub.ai.dto.TicketClassificationSuggestion;
import com.resolvehub.ai.service.OpenAiCompatibleTicketAiClassifier;
import com.resolvehub.ticket.domain.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class OpenAiCompatibleTicketAiClassifierTest {

    @Test
    void invalidAiResponseIsHandledCleanly() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}", MediaType.APPLICATION_JSON));

        OpenAiCompatibleTicketAiClassifier classifier = new OpenAiCompatibleTicketAiClassifier(
                builder.build(),
                new ObjectMapper(),
                properties().getOpenaiCompatible()
        );

        Ticket ticket = ticket("Login broken", "Cannot login, timeout error");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> classifier.classify(ticket));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());

        server.verify();
    }

    @Test
    void providerHttpFailureIsHandledCleanly() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        OpenAiCompatibleTicketAiClassifier classifier = new OpenAiCompatibleTicketAiClassifier(
                builder.build(),
                new ObjectMapper(),
                properties().getOpenaiCompatible()
        );

        Ticket ticket = ticket("Billing issue", "Charged twice");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> classifier.classify(ticket));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());

        server.verify();
    }

    @Test
    void validAiResponseIsParsedIntoSuggestion() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String responseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\\"category\\\":\\\"TECHNICAL\\\",\\\"priority\\\":\\\"HIGH\\\",\\\"reasoning\\\":\\\"Login failures indicate a technical incident.\\\"}"
                      }
                    }
                  ]
                }
                """;

        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        OpenAiCompatibleTicketAiClassifier classifier = new OpenAiCompatibleTicketAiClassifier(
                builder.build(),
                new ObjectMapper(),
                properties().getOpenaiCompatible()
        );

        TicketClassificationSuggestion suggestion = classifier.classify(ticket("Login error", "Cannot login after deploy"));

        assertEquals("TECHNICAL", suggestion.suggestedCategory().name());
        assertEquals("HIGH", suggestion.suggestedPriority().name());
        assertEquals("Login failures indicate a technical incident.", suggestion.reasoning());

        server.verify();
    }

    private AiProviderProperties properties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setProvider("openai-compatible");
        properties.getOpenaiCompatible().setBaseUrl("http://localhost:11434/v1");
        properties.getOpenaiCompatible().setApiKey("test-key");
        properties.getOpenaiCompatible().setModel("test-model");
        properties.getOpenaiCompatible().setTimeoutSeconds(5);
        return properties;
    }

    private Ticket ticket(String title, String description) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        return ticket;
    }
}
