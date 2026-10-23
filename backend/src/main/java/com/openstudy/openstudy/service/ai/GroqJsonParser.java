package com.openstudy.openstudy.service.ai;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GroqJsonParser {
    private static final int MAX_RESPONSE_CHARS = 1_000_000;
    private final ObjectMapper mapper;

    public GroqJsonParser(ObjectMapper mapper) { this.mapper = mapper; }

    public JsonNode parsePayload(String body) {
        if (body == null || body.isBlank()) throw invalid("Groq returned an empty response.");
        if (body.length() > MAX_RESPONSE_CHARS) throw invalid("Groq response exceeded the size limit.");
        try {
            JsonNode payload = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(body);
            if (payload == null || (!payload.isObject() && !payload.isArray())) throw invalid("Groq returned malformed JSON.");
            return payload;
        } catch (AiServiceException ex) { throw ex; }
        catch (Exception ex) { throw invalid("Groq returned malformed JSON."); }
    }

    public String readText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) throw invalid("Groq response was missing a required text field.");
        return value.asText().strip();
    }

    public double readNumber(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isNumber() || !Double.isFinite(value.asDouble())) throw invalid("Groq response was missing a valid numeric field.");
        return value.asDouble();
    }

    public List<String> readStringList(JsonNode node, String field) {
        JsonNode array = node == null ? null : node.get(field);
        if (array == null || !array.isArray() || array.size() > 20) throw invalid("Groq response was missing a valid list field.");
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isTextual() || item.asText().isBlank()) throw invalid("Groq response contained an invalid list item.");
            values.add(item.asText().strip());
        }
        return values;
    }

    private AiServiceException invalid(String message) { return new AiServiceException(AiErrorCode.INVALID_JSON, message); }
}
