package com.resolvehub.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.resolvehub.ai.config.AiProviderProperties;
import com.resolvehub.ai.service.FakeTicketAiClassifier;
import com.resolvehub.ai.service.TicketAiClassifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiProviderConfigurationTest {

    @Autowired
    private TicketAiClassifier ticketAiClassifier;

    @Autowired
    private AiProviderProperties aiProviderProperties;

    @Test
    void fakeProviderIsDefaultInTestProfile() {
        assertInstanceOf(FakeTicketAiClassifier.class, ticketAiClassifier);
        assertEquals("fake", aiProviderProperties.getProvider());
    }
}
