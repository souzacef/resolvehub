package com.resolvehub.ai;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.resolvehub.ai.service.OpenAiCompatibleTicketAiClassifier;
import com.resolvehub.ai.service.TicketAiClassifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "resolvehub.ai.provider=openai-compatible",
        "resolvehub.ai.openai-compatible.base-url=http://localhost:11434/v1",
        "resolvehub.ai.openai-compatible.api-key=test-key",
        "resolvehub.ai.openai-compatible.model=test-model",
        "resolvehub.ai.openai-compatible.timeout-seconds=5"
})
@ActiveProfiles("test")
class OpenAiCompatibleProviderConfigurationTest {

    @Autowired
    private TicketAiClassifier ticketAiClassifier;

    @Test
    void openAiCompatibleProviderBeanCanBeConfigured() {
        assertInstanceOf(OpenAiCompatibleTicketAiClassifier.class, ticketAiClassifier);
    }
}
