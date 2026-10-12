package com.openstudy.openstudy.service.ai;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GeminiJsonParser {
    private static final int MAX_RESPONSE_CHARS = 1_000_000;
    private static final Set<String> BLOCKED_REASONS = Set.of(
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY");
    private final ObjectMapper objectMapper;

    public GeminiJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractText(String rawBody) {
        JsonNode root = parseJson(rawBody);
        requireObject(root);
        if (root.has("error")) {
            throw mapApiError(root.get("error"));
        }
        JsonNode feedback = root.get("promptFeedback");
        if (feedback != null) {
            requireObject(feedback);
            if (feedback.has("blockReason") && !"BLOCK_REASON_UNSPECIFIED".equals(readText(feedback, "blockReason"))) {
                throw unexpected("Gemini blocked the prompt.");
            }
        }
        JsonNode candidates = root.get("candidates");
        if (candidates == null || (candidates.isArray() && candidates.isEmpty())) {
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Gemini returned no candidates.");
        }
        if (!candidates.isArray()) {
            throw malformed();
        }
        JsonNode candidate = candidates.get(0);
        requireObject(candidate);
        String reason = readText(candidate, "finishReason");
        if ("MAX_TOKENS".equals(reason)) {
            throw unexpected("Gemini response was truncated.");
        }
        if (BLOCKED_REASONS.contains(reason)) {
            throw unexpected("Gemini blocked the response.");
        }
        if (!"STOP".equals(reason)) {
            throw unexpected("Gemini response did not complete successfully.");
        }
        JsonNode ratings = candidate.get("safetyRatings");
        if (ratings != null) {
            if (!ratings.isArray()) {
                throw malformed();
            }
            for (JsonNode rating : ratings) {
                requireObject(rating);
                if (rating.has("blocked")) {
                    if (!rating.get("blocked").isBoolean()) {
                        throw malformed();
                    }
                    if (rating.get("blocked").asBoolean()) {
                        throw unexpected("Gemini blocked the response.");
                    }
                }
            }
        }
        JsonNode content = candidate.get("content");
        requireObject(content);
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray()) {
            throw malformed();
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            requireObject(part);
            if (part.has("thought") && !part.get("thought").isBoolean()) {
                throw malformed();
            }
            if (part.has("text")) {
                if (!part.get("text").isTextual()) {
                    throw malformed();
                }
                // Thinking text is not the JSON answer.
                if (!part.path("thought").asBoolean(false)) {
                    text.append(part.get("text").asText());
                }
            } else if (!part.path("thought").asBoolean(false)) {
                throw malformed();
            }
        }
        if (text.toString().isBlank()) {
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Gemini candidate contained no answer text.");
        }
        String result = text.toString().strip();
        if (result.startsWith("```")) {
            int newline = result.indexOf('\n');
            if (newline > 0 && result.endsWith("```")) {
                result = result.substring(newline + 1, result.length() - 3).strip();
            }
        }
        return result;
    }

    public JsonNode parsePayload(String textPayload) {
        JsonNode payload = parseJson(textPayload);
        if (!payload.isObject() && !payload.isArray()) {
            throw malformed();
        }
        return payload;
    }

    public String readText(JsonNode node, String field) {
        requireObject(node);
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw malformed();
        }
        return value.asText().strip();
    }

    public double readNumber(JsonNode node, String field) {
        requireObject(node);
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber() || !Double.isFinite(value.asDouble())) {
            throw malformed();
        }
        return value.asDouble();
    }

    public List<String> readStringList(JsonNode node, String field) {
        requireObject(node);
        JsonNode array = node.get(field);
        if (array == null || !array.isArray() || array.size() > 20) {
            throw malformed();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw malformed();
            }
            values.add(item.asText().strip());
        }
        return values;
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Gemini returned an empty body.");
        }
        if (body.length() > MAX_RESPONSE_CHARS) {
            throw unexpected("Gemini response exceeded the size limit.");
        }
        try {
            JsonNode root = objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(body);
            if (root == null || root.isNull()) {
                throw malformed();
            }
            return root;
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            // Jackson messages may contain upstream data; do not retain them.
            throw new AiServiceException(AiErrorCode.INVALID_JSON, "Gemini returned invalid JSON.");
        }
    }

    private void requireObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw malformed();
        }
    }

    private AiServiceException mapApiError(JsonNode error) {
        requireObject(error);
        int code = error.path("code").asInt(0);
        String message = error.path("message").asText("").toLowerCase(Locale.ROOT);
        String status = error.path("status").asText("");
        if (code == 401 || code == 403 || status.contains("UNAUTHENTICATED") || message.contains("api key")) {
            return new AiServiceException(AiErrorCode.INVALID_API_KEY, "Gemini rejected the configured API key.");
        }
        if (code == 429 || status.contains("RESOURCE_EXHAUSTED")) {
            return new AiServiceException(AiErrorCode.RATE_LIMITED, "Gemini rate limit reached. Try again later.");
        }
        return unexpected("Gemini returned an API error.");
    }

    private AiServiceException malformed() {
        return new AiServiceException(AiErrorCode.INVALID_JSON, "Gemini response fields were malformed or missing.");
    }

    private AiServiceException unexpected(String message) {
        return new AiServiceException(AiErrorCode.UNEXPECTED_RESPONSE, message);
    }
}
