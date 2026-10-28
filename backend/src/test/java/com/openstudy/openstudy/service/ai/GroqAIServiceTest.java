package com.openstudy.openstudy.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.openstudy.openstudy.config.GroqProperties;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GroqAIServiceTest {
    private static final String CHAT_COMPLETIONS = "https://api.groq.com/openai/v1/chat/completions";

    @Test
    void missingKeyFailsBeforeMakingAnHttpRequest() {
        var fixture = service("");

        assertThatThrownBy(() -> fixture.service().generateMCQs(request()))
                .isInstanceOf(AiServiceException.class)
                .satisfies(error -> assertThat(((AiServiceException) error).getErrorCode())
                        .isEqualTo(AiErrorCode.MISSING_API_KEY));
        fixture.server().verify();
    }

    @Test
    void rejectedKeyIsMappedWithoutExposingTheUpstreamBody() {
        var fixture = service("unit-test-key");
        fixture.server().expect(requestTo(CHAT_COMPLETIONS))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"private credential detail\"}"));

        assertThatThrownBy(() -> fixture.service().generateMCQs(request()))
                .isInstanceOf(AiServiceException.class)
                .satisfies(error -> {
                    var aiError = (AiServiceException) error;
                    assertThat(aiError.getErrorCode()).isEqualTo(AiErrorCode.INVALID_API_KEY);
                    assertThat(aiError.getMessage()).doesNotContain("unit-test-key", "private credential detail");
                });
        fixture.server().verify();
    }

    @Test
    void mapsGroqCompletionIntoReviewedMcqDtoFields() {
        var fixture = service("unit-test-key");
        String content = """
                {"questions":[{"questionText":"What is a stack?","options":["LIFO","FIFO","Tree","Graph"],"correctOption":"LIFO","explanation":"A stack uses last in, first out ordering.","difficulty":"EASY"}]}
                """.trim().replace("\"", "\\\"");
        String envelope = """
                {"choices":[{"finish_reason":"stop","message":{"content":"%s"}}]}
                """.formatted(content);
        fixture.server().expect(requestTo(CHAT_COMPLETIONS))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(envelope, MediaType.APPLICATION_JSON));

        var generated = fixture.service().generateMCQs(request());

        assertThat(generated).hasSize(1);
        assertThat(generated.getFirst().getQuestionText()).isEqualTo("What is a stack?");
        assertThat(generated.getFirst().getOptions()).containsExactly("LIFO", "FIFO", "Tree", "Graph");
        assertThat(generated.getFirst().getCorrectOption()).isEqualTo("LIFO");
        assertThat(generated.getFirst().getExplanation()).contains("last in, first out");
        assertThat(generated.getFirst().getSource()).isEqualTo(AiResponseValidator.SOURCE_GROQ);
        fixture.server().verify();
    }

    @Test
    void identifiesDecommissionedModelFromSafeErrorClassification() {
        var fixture = service("unit-test-key");
        fixture.server().expect(requestTo(CHAT_COMPLETIONS))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"model_decommissioned\",\"message\":\"private detail\"}}"));

        assertThatThrownBy(() -> fixture.service().generateMCQs(request()))
                .isInstanceOf(AiServiceException.class)
                .satisfies(error -> assertThat(((AiServiceException) error).getErrorCode())
                        .isEqualTo(AiErrorCode.INVALID_MODEL));
        fixture.server().verify();
    }

    private Fixture service(String apiKey) {
        var properties = new GroqProperties();
        properties.setApiKey(apiKey);
        var builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new GroqAIService(builder.build(), properties,
                new GroqJsonParser(new ObjectMapper()), new AiResponseValidator());
        return new Fixture(service, server);
    }

    private MCQGenerationRequest request() {
        var request = new MCQGenerationRequest();
        request.setSubject("Computer Science");
        request.setTopic("Stacks");
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(1);
        return request;
    }

    private record Fixture(GroqAIService service, MockRestServiceServer server) { }
}
