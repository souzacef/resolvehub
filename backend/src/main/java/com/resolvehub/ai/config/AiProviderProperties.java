package com.resolvehub.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resolvehub.ai")
public class AiProviderProperties {

    private String provider = "fake";
    private OpenAiCompatibleProperties openaiCompatible = new OpenAiCompatibleProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAiCompatibleProperties getOpenaiCompatible() {
        return openaiCompatible;
    }

    public void setOpenaiCompatible(OpenAiCompatibleProperties openaiCompatible) {
        this.openaiCompatible = openaiCompatible;
    }

    public static class OpenAiCompatibleProperties {

        private String baseUrl = "http://127.0.0.1:11434/v1";
        private String apiKey = "ollama";
        private String model = "llama3.1:8b";
        private int timeoutSeconds = 20;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
