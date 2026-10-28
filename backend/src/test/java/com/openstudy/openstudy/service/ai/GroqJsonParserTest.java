package com.openstudy.openstudy.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GroqJsonParserTest {

    private final GroqJsonParser parser = new GroqJsonParser(new ObjectMapper());

    @Test
    void extractsChoiceContent() {
        String body = """
                {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"{\\"ok\\":true}"}}]}
                """;
        assertThat(parser.extractText(body)).isEqualTo("{\"ok\":true}");
    }

    @Test
    void mapsInvalidApiKeyError() {
        String body = """
                {"error":{"message":"Invalid API Key","type":"invalid_request_error","code":"invalid_api_key"}}
                """;
        assertThatThrownBy(() -> parser.extractText(body))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.INVALID_API_KEY);
    }

    @Test
    void mapsRateLimitError() {
        String body = """
                {"error":{"message":"quota","type":"tokens","code":"rate_limit_exceeded"}}
                """;
        assertThatThrownBy(() -> parser.extractText(body))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.RATE_LIMITED);
    }

    @Test
    void rejectsEmptyChoices() {
        assertThatThrownBy(() -> parser.extractText("{\"choices\":[]}"))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.extractText("{not-json"))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.INVALID_JSON);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"length", "content_filter", "tool_calls", "other"})
    void rejectsIncompleteOrBlockedChoicesEvenWithValidText(String reason) {
        String body = "{\"choices\":[{\"finish_reason\":\"" + reason
                + "\",\"message\":{\"content\":\"{}\"}}]}";
        assertThatThrownBy(() -> parser.extractText(body)).isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.UNEXPECTED_RESPONSE);
    }

    @Test
    void reportsTruncatedAndBlockedResponses() {
        assertThatThrownBy(() -> parser.extractText("{\"choices\":[{\"finish_reason\":\"length\","
                + "\"message\":{\"content\":\"{\"}}]}"))
                .isInstanceOf(AiServiceException.class).hasMessage("Groq response was truncated.");
        assertThatThrownBy(() -> parser.extractText("{\"choices\":[{\"finish_reason\":\"content_filter\","
                + "\"message\":{\"content\":\"\"}}]}"))
                .isInstanceOf(AiServiceException.class).hasMessage("Groq blocked the response.");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "null", "[]", "{\"choices\":{}}", "{\"choices\":[null]}",
            "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}",
            "{\"choices\":[{\"finish_reason\":3}]}",
            "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":3}}]}",
            "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":[]}]}",
            "{\"choices\":[{\"finish_reason\":\"stop\"}]}"
    })
    void rejectsMalformedEnvelopeFields(String body) {
        assertThatThrownBy(() -> parser.extractText(body)).isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.INVALID_JSON);
    }

    @Test
    void rejectsMissingContentAndIgnoresReasoningField() {
        assertThatThrownBy(() -> parser.extractText("{\"choices\":[{\"finish_reason\":\"stop\","
                + "\"message\":{\"content\":null}}]}"))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        assertThat(parser.extractText("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{"
                + "\"reasoning\":\"Internal thinking\",\"content\":\"{}\"}}]}"))
                .isEqualTo("{}");
    }

    @Test
    void validatesPayloadTypesAndRejectsTrailingJson() {
        for (String payload : java.util.List.of("null", "true", "3", "{} {}")) {
            assertThatThrownBy(() -> parser.parsePayload(payload)).isInstanceOf(AiServiceException.class);
        }
        var payload = parser.parsePayload("{\"score\":\"5\",\"feedback\":3,\"options\":[\"A\",null]}");
        assertThatThrownBy(() -> parser.readNumber(payload, "score")).isInstanceOf(AiServiceException.class);
        assertThatThrownBy(() -> parser.readText(payload, "feedback")).isInstanceOf(AiServiceException.class);
        assertThatThrownBy(() -> parser.readStringList(payload, "options")).isInstanceOf(AiServiceException.class);
        assertThatThrownBy(() -> parser.readStringList(payload, "missing")).isInstanceOf(AiServiceException.class);
    }

    @Test
    void sanitizesErrorMessagesAndLimitsResponseSize() {
        assertThatThrownBy(() -> parser.extractText("{\"error\":{\"code\":\"server_error\",\"message\":\"PRIVATE_RESPONSE_MARKER\"}}"))
                .isInstanceOf(AiServiceException.class).hasMessage("Groq returned an API error.").hasNoCause();
        assertThatThrownBy(() -> parser.extractText("x".repeat(1_000_001)))
                .isInstanceOf(AiServiceException.class).hasMessage("Groq response exceeded the size limit.");
        assertThatThrownBy(() -> parser.parsePayload("{PRIVATE_RESPONSE_MARKER"))
                .isInstanceOf(AiServiceException.class).hasNoCause();
    }
}
