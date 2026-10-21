package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.util.Set;

@ConfigurationProperties(prefix = "openstudy.ai.gemini")
public class GeminiProperties {

    private String apiKey = "";
    private String model = "gemini-3.8-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int timeoutMs = 30000;
    private boolean diagnosticsEnabled;

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.strip();
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
        String key = getApiKey();
        return !key.isBlank() && key.length() <= 512 && key.chars().noneMatch(Character::isISOControl)
                && !Set.of("YOUR_API_KEY_HERE", "replace_with_your_gemini_api_key", "your-real-key").contains(key);
    }

    public boolean isDiagnosticsEnabled() { return diagnosticsEnabled; }
    public void setDiagnosticsEnabled(boolean value) { diagnosticsEnabled = value; }

    /** Validate nonsecret transport configuration without ever echoing rejected values. */
    public void validateTransport() {
        if (model == null || !model.matches("gemini-[A-Za-z0-9._-]{1,90}")) {
            throw new IllegalArgumentException("Gemini model must be a Gemini model identifier.");
        }
        if (timeoutMs < 1000 || timeoutMs > 120000) {
            throw new IllegalArgumentException("Gemini timeout must be between 1000 and 120000 milliseconds.");
        }
        try {
            URI uri = URI.create(baseUrl);
            if (!"https".equals(uri.getScheme()) || !"generativelanguage.googleapis.com".equals(uri.getHost())
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !Set.of("/v1beta", "/v1").contains(uri.getPath())) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Gemini base URL must use the official Google HTTPS API endpoint.");
        }
    }

    @Override
    public String toString() {
        return "GeminiProperties{timeoutMs=%d, keyConfigured=%s}".formatted(timeoutMs, hasApiKey());
    }
}
