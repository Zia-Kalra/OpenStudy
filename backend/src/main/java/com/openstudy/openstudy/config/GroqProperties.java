package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.ai.groq")
public class GroqProperties {
    private String apiKey = "";
    private String model = "openai/gpt-oss-20b";
    private int timeoutMs = 30000;

    public String getApiKey() { return apiKey == null ? "" : apiKey.strip(); }
    public void setApiKey(String value) { apiKey = value; }
    public String getModel() { return model; }
    public void setModel(String value) { model = value; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int value) { timeoutMs = value; }
    public boolean hasApiKey() {
        return !getApiKey().isBlank() && getApiKey().length() <= 512
                && getApiKey().chars().noneMatch(Character::isISOControl);
    }
    public void validateTransport() {
        if (model == null || !model.matches("[A-Za-z0-9][A-Za-z0-9._/-]{0,119}")) {
            throw new IllegalArgumentException("Configure a valid GROQ_MODEL identifier.");
        }
        if (timeoutMs < 1000 || timeoutMs > 120000) {
            throw new IllegalArgumentException("Groq timeout must be between 1000 and 120000 milliseconds.");
        }
    }
    @Override
    public String toString() {
        return "GroqProperties{keyConfigured=" + hasApiKey() + ", timeoutMs=" + timeoutMs + "}";
    }
}
