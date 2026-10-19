package com.openstudy.openstudy.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiJsonParserTest {

    private final GeminiJsonParser parser = new GeminiJsonParser(new ObjectMapper());

    @Test
    void extractsCandidateText() {
        String body = """
                {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{\\"ok\\":true}"}]}}]}
                """;
        assertThat(parser.extractText(body)).isEqualTo("{\"ok\":true}");
    }

    @Test
    void mapsInvalidApiKeyError() {
        String body = """
                {"error":{"code":400,"message":"API key not valid","status":"INVALID_ARGUMENT"}}
                """;
        assertThatThrownBy(() -> parser.extractText(body))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.INVALID_API_KEY);
    }

    @Test
    void mapsRateLimitError() {
        String body = """
                {"error":{"code":429,"message":"quota","status":"RESOURCE_EXHAUSTED"}}
                """;
        assertThatThrownBy(() -> parser.extractText(body))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.RATE_LIMITED);
    }

    @Test
    void rejectsEmptyCandidates() {
        assertThatThrownBy(() -> parser.extractText("{\"candidates\":[]}"))
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
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "MAX_TOKENS", "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "OTHER", "FINISH_REASON_UNSPECIFIED"
    })
    void rejectsIncompleteOrBlockedCandidatesEvenWithValidText(String reason) {
        String body = "{\"candidates\":[{\"finishReason\":\"" + reason
                + "\",\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}";
        assertThatThrownBy(() -> parser.extractText(body)).isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.UNEXPECTED_RESPONSE);
    }

    @Test
    void rejectsPromptBlockingAndBlockedSafetyRating() {
        assertThatThrownBy(() -> parser.extractText("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"))
                .isInstanceOf(AiServiceException.class).hasMessage("Gemini blocked the prompt.");
        assertThatThrownBy(() -> parser.extractText("{\"candidates\":[{\"finishReason\":\"STOP\","
                + "\"safetyRatings\":[{\"blocked\":true}]}]}"))
                .isInstanceOf(AiServiceException.class).hasMessage("Gemini blocked the response.");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "null", "[]", "{\"candidates\":{}}", "{\"candidates\":[null]}",
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}",
            "{\"candidates\":[{\"finishReason\":3}]}",
            "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":3}]}}]}",
            "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":{}}}]}",
            "{\"candidates\":[{\"finishReason\":\"STOP\",\"safetyRatings\":[{\"blocked\":\"true\"}]}]}"
    })
    void rejectsMalformedEnvelopeFields(String body) {
        assertThatThrownBy(() -> parser.extractText(body)).isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.INVALID_JSON);
    }

    @Test
    void skipsThinkingPartsAndJoinsAnswerParts() {
        assertThat(parser.extractText("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":["
                + "{\"thought\":true,\"text\":\"Internal thinking\"},{\"text\":\"{\"},{\"text\":\"}\"}]}}]}"))
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
        assertThatThrownBy(() -> parser.extractText("{\"error\":{\"code\":500,\"message\":\"PRIVATE_RESPONSE_MARKER\"}}"))
                .isInstanceOf(AiServiceException.class).hasMessage("Gemini returned an API error.").hasNoCause();
        assertThatThrownBy(() -> parser.extractText("x".repeat(1_000_001)))
                .isInstanceOf(AiServiceException.class).hasMessage("Gemini response exceeded the size limit.");
        assertThatThrownBy(() -> parser.parsePayload("{PRIVATE_RESPONSE_MARKER"))
                .isInstanceOf(AiServiceException.class).hasNoCause();
    }
}
