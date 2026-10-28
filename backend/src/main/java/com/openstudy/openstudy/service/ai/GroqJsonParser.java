package com.openstudy.openstudy.service.ai;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class GroqJsonParser {
    private static final int MAX_RESPONSE_CHARS = 1_000_000;
    private final ObjectMapper objectMapper;

    public GroqJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractText(String rawBody) {
        JsonNode root = parseJson(rawBody);
        requireObject(root);
        if (root.has("error")) {
            throw mapApiError(root.get("error"));
        }
        JsonNode choices = root.get("choices");
        if (choices == null || (choices.isArray() && choices.isEmpty())) {
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Groq returned no choices.");
        }
        if (!choices.isArray()) {
            throw malformed();
        }
        JsonNode choice = choices.get(0);
        requireObject(choice);
        String reason = readText(choice, "finish_reason");
        if ("length".equals(reason)) {
            throw unexpected("Groq response was truncated.");
        }
        if ("content_filter".equals(reason)) {
            throw unexpected("Groq blocked the response.");
        }
        if (!"stop".equals(reason)) {
            throw unexpected("Groq response did not complete successfully.");
        }
        JsonNode message = choice.get("message");
        requireObject(message);
        JsonNode content = message.get("content");
        if (content == null || content.isNull()) {
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Groq choice contained no answer text.");
        }
        if (!content.isTextual()) {
            throw malformed();
        }
        // Reasoning models return thinking in a separate "reasoning" field; only content is the JSON answer.
        String result = content.asText().strip();
        if (result.isEmpty()) {
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Groq choice contained no answer text.");
        }
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
            throw new AiServiceException(AiErrorCode.EMPTY_RESPONSE, "Groq returned an empty body.");
        }
        if (body.length() > MAX_RESPONSE_CHARS) {
            throw unexpected("Groq response exceeded the size limit.");
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
            throw new AiServiceException(AiErrorCode.INVALID_JSON, "Groq returned invalid JSON.");
        }
    }

    private void requireObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw malformed();
        }
    }

    private AiServiceException mapApiError(JsonNode error) {
        requireObject(error);
        String code = error.path("code").asText("").toLowerCase(Locale.ROOT);
        String type = error.path("type").asText("").toLowerCase(Locale.ROOT);
        String message = error.path("message").asText("").toLowerCase(Locale.ROOT);
        if (code.equals("invalid_api_key") || code.equals("401") || code.equals("403")
                || type.contains("authentication") || message.contains("api key")) {
            return new AiServiceException(AiErrorCode.INVALID_API_KEY, "Groq rejected the configured API key.");
        }
        if (code.equals("rate_limit_exceeded") || code.equals("429") || type.contains("rate_limit")) {
            return new AiServiceException(AiErrorCode.RATE_LIMITED, "Groq rate limit reached. Try again later.");
        }
        return unexpected("Groq returned an API error.");
    }

    private AiServiceException malformed() {
        return new AiServiceException(AiErrorCode.INVALID_JSON, "Groq response fields were malformed or missing.");
    }

    private AiServiceException unexpected(String message) {
        return new AiServiceException(AiErrorCode.UNEXPECTED_RESPONSE, message);
    }
}
