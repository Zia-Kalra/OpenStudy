package com.openstudy.openstudy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class GeminiPropertiesTest {
    @Test
    void rejectsMissingPlaceholderAndControlCharacterKeysWithoutEchoingThem() {
        var properties = new GeminiProperties();
        for (String value : new String[] { null, "", " ", "YOUR_API_KEY_HERE", "replace_with_your_gemini_api_key", "key\nheader" }) {
            properties.setApiKey(value);
            assertThat(properties.hasApiKey()).isFalse();
        }
        properties.setApiKey("  SYNTHETIC_TEST_KEY  ");
        assertThat(properties.getApiKey()).isEqualTo("SYNTHETIC_TEST_KEY");
        assertThat(properties.hasApiKey()).isTrue();
        assertThat(properties.toString()).doesNotContain("SYNTHETIC_TEST_KEY");
    }

    @Test
    void preventsSendingKeysToNonGoogleOrInsecureEndpoints() {
        var properties = new GeminiProperties();
        for (String url : new String[] { "http://generativelanguage.googleapis.com/v1beta", "https://example.invalid/v1beta",
                "https://generativelanguage.googleapis.com.evil.invalid/v1beta", "https://user:secret@generativelanguage.googleapis.com/v1beta",
                "https://generativelanguage.googleapis.com/v1beta?key=SYNTHETIC_TEST_KEY", "https://generativelanguage.googleapis.com:8443/v1beta", "invalid" }) {
            properties.setBaseUrl(url);
            assertThatThrownBy(properties::validateTransport).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Gemini base URL must use the official Google HTTPS API endpoint.");
        }
    }

    @Test
    void validatesModelAndTimeoutConfiguration() {
        var properties = new GeminiProperties();
        properties.validateTransport();
        for (int timeout : new int[] { -1, 0, 999, 120001 }) {
            properties.setTimeoutMs(timeout);
            assertThatThrownBy(properties::validateTransport).isInstanceOf(IllegalArgumentException.class);
        }
        properties.setTimeoutMs(30000);
        properties.setModel("https://example.invalid/model");
        assertThatThrownBy(properties::validateTransport).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Gemini model must be a Gemini model identifier.");
        properties.setModel("gemini-3.8-flash");
        properties.setBaseUrl("https://generativelanguage.googleapis.com/v1");
        properties.validateTransport();
    }
}
