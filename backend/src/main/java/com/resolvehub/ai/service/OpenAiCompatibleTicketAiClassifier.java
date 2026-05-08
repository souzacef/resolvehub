package com.resolvehub.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvehub.ai.config.AiProviderProperties;
import com.resolvehub.ai.dto.TicketClassificationSuggestion;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(prefix = "resolvehub.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleTicketAiClassifier implements TicketAiClassifier {

    private static final String SYSTEM_PROMPT = "You classify support tickets. Return strict JSON only with keys: "
            + "category, priority, reasoning. Category must be one of BILLING, TECHNICAL, ACCOUNT, FEATURE_REQUEST, "
            + "SECURITY, OTHER. Priority must be one of LOW, MEDIUM, HIGH, URGENT.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProviderProperties.OpenAiCompatibleProperties config;

    @Autowired
    public OpenAiCompatibleTicketAiClassifier(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AiProviderProperties properties
    ) {
        this(
                buildRestClient(restClientBuilder, properties.getOpenaiCompatible()),
                objectMapper,
                properties.getOpenaiCompatible()
        );
    }

    public OpenAiCompatibleTicketAiClassifier(
            RestClient restClient,
            ObjectMapper objectMapper,
            AiProviderProperties.OpenAiCompatibleProperties config
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.config = config;
        validateConfiguration(this.config);
    }

    private static RestClient buildRestClient(
            RestClient.Builder restClientBuilder,
            AiProviderProperties.OpenAiCompatibleProperties config
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        RestClient.Builder builder = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .baseUrl(config.getBaseUrl());

        if (StringUtils.hasText(config.getApiKey())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey());
        }

        return builder.build();
    }

    @Override
    public TicketClassificationSuggestion classify(Ticket ticket) {
        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildUserPrompt(ticket))
                )
        );

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw providerFailure("AI provider returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw providerFailure("AI provider request failed", ex);
        }

        if (!StringUtils.hasText(rawResponse)) {
            throw providerFailure("AI provider returned an empty response", null);
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (!StringUtils.hasText(content)) {
                throw providerFailure("AI provider response missing message content", null);
            }

            JsonNode suggestionNode = objectMapper.readTree(extractJsonObject(content));
            TicketCategory category = parseCategory(requiredText(suggestionNode, "category"));
            TicketPriority priority = parsePriority(requiredText(suggestionNode, "priority"));
            String reasoning = requiredText(suggestionNode, "reasoning");

            return new TicketClassificationSuggestion(category, priority, reasoning);
        } catch (JsonProcessingException ex) {
            throw providerFailure("AI provider returned invalid JSON", ex);
        }
    }

    private String buildUserPrompt(Ticket ticket) {
        return "Ticket title: " + ticket.getTitle() + "\n"
                + "Ticket description: " + ticket.getDescription() + "\n"
                + "Return strict JSON only.";
    }

    private String extractJsonObject(String content) {
        String trimmed = content.trim();

        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < 0 || lastBrace <= firstBrace) {
            throw providerFailure("AI provider response did not contain a JSON object", null);
        }

        return trimmed.substring(firstBrace, lastBrace + 1);
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw providerFailure("AI provider response missing field: " + fieldName, null);
        }
        return value.trim();
    }

    private TicketCategory parseCategory(String categoryValue) {
        try {
            return TicketCategory.valueOf(categoryValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw providerFailure("AI provider returned unsupported category: " + categoryValue, ex);
        }
    }

    private TicketPriority parsePriority(String priorityValue) {
        try {
            return TicketPriority.valueOf(priorityValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw providerFailure("AI provider returned unsupported priority: " + priorityValue, ex);
        }
    }

    private void validateConfiguration(AiProviderProperties.OpenAiCompatibleProperties properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("resolvehub.ai.openai-compatible.base-url must not be blank");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("resolvehub.ai.openai-compatible.model must not be blank");
        }
        if (properties.getTimeoutSeconds() <= 0) {
            throw new IllegalStateException("resolvehub.ai.openai-compatible.timeout-seconds must be positive");
        }
    }

    private ResponseStatusException providerFailure(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, cause);
    }
}
