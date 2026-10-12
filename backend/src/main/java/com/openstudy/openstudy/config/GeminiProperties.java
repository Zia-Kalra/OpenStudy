package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.ai.gemini")
public class GeminiProperties {

    private String apiKey = "";
    private String model = "gemini-3.8-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int timeoutMs = 30000;

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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "GeminiProperties{model='%s', baseUrl='%s', timeoutMs=%d, keyConfigured=%s}"
                .formatted(model, baseUrl, timeoutMs, hasApiKey());
    }
}
