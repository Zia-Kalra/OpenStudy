package com.openstudy.openstudy.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import tools.jackson.databind.ObjectMapper;
import com.openstudy.openstudy.config.GeminiProperties;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiAIServiceTest {

    private MockRestServiceServer server;
    private GeminiAIService service;
    private GeminiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setApiKey("test-key");
        properties.setModel("gemini-3.8-flash");
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GeminiAIService(
                builder.build(),
                properties,
                new GeminiJsonParser(new ObjectMapper()),
                new AiResponseValidator());
    }

    @Test
    void missingApiKeyFailsBeforeHttpCall() {
        properties.setApiKey(" ");
        assertThatThrownBy(() -> service.callGemini("prompt"))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.MISSING_API_KEY);
    }

    @Test
    void invalidApiKeyMapsToDedicatedError() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> service.callGemini("prompt"))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.INVALID_API_KEY);
        server.verify();
    }

    @Test
    void successfulMcqResponseIsValidated() {
        String geminiBody = """
                {
                  "candidates": [
                    {
                      "finishReason": "STOP",
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"questions\\":[{\\"questionText\\":\\"What is O(n)?\\",\\"options\\":[\\"Linear\\",\\"Constant\\",\\"Log\\",\\"Quadratic\\"],\\"correctOption\\":\\"Linear\\",\\"explanation\\":\\"Linear scan\\",\\"difficulty\\":\\"EASY\\"}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess(geminiBody, MediaType.APPLICATION_JSON));

        MCQGenerationRequest request = new MCQGenerationRequest();
        request.setSubject("Algorithms");
        request.setTopic("Complexity");
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(1);

        var questions = service.generateMCQs(request);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getCorrectOption()).isEqualTo("Linear");
        assertThat(questions.get(0).getSource()).isEqualTo(AiResponseValidator.SOURCE_GEMINI);
        server.verify();
    }

    @Test
    void serverErrorIsMapped() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.callGemini("prompt"))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.NETWORK_FAILURE);
        server.verify();
    }

    private void respondWithPayload(java.util.Map<String, Object> payload) {
        var mapper = new ObjectMapper();
        String text = mapper.writeValueAsString(payload);
        String body = mapper.writeValueAsString(java.util.Map.of("candidates", java.util.List.of(
                java.util.Map.of("finishReason", "STOP", "content", java.util.Map.of("parts", java.util.List.of(java.util.Map.of("text", text)))))));
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void evaluatesAssignmentAndKeepsTeacherAuthority() {
        respondWithPayload(java.util.Map.of("suggestedScore", 12, "feedback", "Clear answer",
                "strengths", java.util.List.of("Clarity"), "areasForImprovement", java.util.List.of()));
        var result = service.evaluateAssignmentAnswer(AiRequestValidationTest.evaluation());
        assertThat(result.getSuggestedScore()).isEqualTo(10);
        assertThat(result.isScoreAdjusted()).isTrue();
        assertThat(result.getSource()).isEqualTo("GEMINI");
        assertThat(result.getTeacherAuthorityNote()).contains("teacher retains final grading authority");
        server.verify();
    }

    @Test
    void refusesCoercedEvaluationFields() {
        respondWithPayload(java.util.Map.of("suggestedScore", "5", "feedback", "Feedback",
                "strengths", java.util.List.of(), "areasForImprovement", java.util.List.of()));
        assertThatThrownBy(() -> service.evaluateAssignmentAnswer(AiRequestValidationTest.evaluation()))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.INVALID_JSON);
        server.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"429,RATE_LIMITED", "408,TIMEOUT", "400,UNEXPECTED_RESPONSE"})
    void mapsHttpErrorsWithoutExposingUpstreamBody(int status, AiErrorCode expected) {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.valueOf(status)).body("PRIVATE_RESPONSE_MARKER"));
        assertThatThrownBy(() -> service.callGemini("prompt"))
                .isInstanceOf(AiServiceException.class).hasNoCause()
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(expected);
        server.verify();
    }

    @Test
    void mapsSocketTimeoutAndNetworkErrors() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withException(new java.net.SocketTimeoutException("PRIVATE_RESPONSE_MARKER")));
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withException(new java.io.IOException("PRIVATE_RESPONSE_MARKER")));
        assertThatThrownBy(() -> service.callGemini("prompt")).isInstanceOf(AiServiceException.class).hasNoCause()
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.TIMEOUT);
        assertThatThrownBy(() -> service.callGemini("prompt")).isInstanceOf(AiServiceException.class).hasNoCause()
                .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.NETWORK_FAILURE);
        server.verify();
    }
}
