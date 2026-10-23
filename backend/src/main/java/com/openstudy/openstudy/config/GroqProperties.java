package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.ai.groq")
public class GroqProperties {
    private String apiKey = "";
    private String model = "openai/gpt-oss-20b";
    private String baseUrl = "https://api.groq.com/openai/v1";
    private int timeoutMs = 60000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public boolean hasApiKey() { return apiKey != null && !apiKey.isBlank(); }
}
