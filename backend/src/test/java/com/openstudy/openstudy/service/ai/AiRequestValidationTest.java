package com.openstudy.openstudy.service.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.openstudy.openstudy.config.GeminiProperties;
import com.openstudy.openstudy.dto.AssignmentEvaluationRequest;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import com.openstudy.openstudy.dto.RubricCriterionDTO;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class AiRequestValidationTest {
    static MCQGenerationRequest mcq() {
        var request = new MCQGenerationRequest();
        request.setSubject("CS");
        request.setTopic("Stacks");
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(1);
        return request;
    }

    static AssignmentEvaluationRequest evaluation() {
        var request = new AssignmentEvaluationRequest();
        request.setQuestionText("Question");
        request.setStudentAnswer("Answer");
        request.setModelAnswer("Model answer");
        request.setMaximumMarks(10);
        return request;
    }

    static Stream<MCQGenerationRequest> invalidMcqs() {
        List<Consumer<MCQGenerationRequest>> changes = List.of(
                r -> r.setSubject(null), r -> r.setSubject(" "), r -> r.setSubject("s".repeat(201)),
                r -> r.setTopic(""), r -> r.setTopic("t".repeat(501)),
                r -> r.setDifficulty(null), r -> r.setDifficulty("INVALID"),
                r -> r.setNumberOfQuestions(null), r -> r.setNumberOfQuestions(0),
                r -> r.setNumberOfQuestions(21), r -> r.setInstructions("i".repeat(2001)));
        return Stream.concat(Stream.of((MCQGenerationRequest) null), changes.stream().map(change -> {
            var request = mcq();
            change.accept(request);
            return request;
        }));
    }

    static Stream<AssignmentEvaluationRequest> invalidEvaluations() {
        List<Consumer<AssignmentEvaluationRequest>> changes = List.of(
                r -> r.setQuestionText(null), r -> r.setQuestionText("q".repeat(10001)),
                r -> r.setStudentAnswer(" "), r -> r.setStudentAnswer("a".repeat(20001)),
                r -> r.setModelAnswer(null), r -> r.setModelAnswer("m".repeat(20001)),
                r -> r.setMaximumMarks(0), r -> r.setMaximumMarks(-1),
                r -> r.setMaximumMarks(Double.NaN), r -> r.setMaximumMarks(Double.POSITIVE_INFINITY),
                r -> r.setMaximumMarks(Double.NEGATIVE_INFINITY),
                r -> r.setRubricCriteria(Arrays.asList((RubricCriterionDTO) null)),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("", "Description", 1))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("Name", "", 1))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("Name", "d".repeat(2001), 1))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("Name", "Description", Double.NaN))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("Name", "Description", Double.POSITIVE_INFINITY))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("Name", "Description", 0))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("Name", "Description", -1))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("A", "Description", 6),
                        new RubricCriterionDTO("B", "Description", 6))),
                r -> r.setRubricCriteria(List.of(new RubricCriterionDTO("A", "Description", 1),
                        new RubricCriterionDTO(" a ", "Description", 1))),
                r -> r.setRubricCriteria(java.util.Collections.nCopies(21, new RubricCriterionDTO("A", "D", 1))));
        return Stream.concat(Stream.of((AssignmentEvaluationRequest) null), changes.stream().map(change -> {
            var request = evaluation();
            change.accept(request);
            return request;
        }));
    }

    private List<AIService> providers() {
        var properties = new GeminiProperties();
        properties.setApiKey("test-key");
        var builder = RestClient.builder().baseUrl("https://example.invalid");
        // No expectations: any HTTP request fails, so validation must precede I/O.
        MockRestServiceServer.bindTo(builder).build();
        var validator = new AiResponseValidator();
        return List.of(new MockAIService(validator), new GeminiAIService(builder.build(), properties,
                new GeminiJsonParser(new ObjectMapper()), validator));
    }

    @ParameterizedTest
    @MethodSource("invalidMcqs")
    void bothProvidersRejectInvalidMcqRequestsBeforeHttp(MCQGenerationRequest request) {
        for (AIService provider : providers()) {
            assertThatThrownBy(() -> provider.generateMCQs(request)).isInstanceOf(AiServiceException.class)
                    .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.VALIDATION_FAILED);
        }
    }

    @ParameterizedTest
    @MethodSource("invalidEvaluations")
    void bothProvidersRejectInvalidEvaluationRequestsBeforeHttp(AssignmentEvaluationRequest request) {
        for (AIService provider : providers()) {
            assertThatThrownBy(() -> provider.evaluateAssignmentAnswer(request)).isInstanceOf(AiServiceException.class)
                    .extracting(ex -> ((AiServiceException) ex).getErrorCode()).isEqualTo(AiErrorCode.VALIDATION_FAILED);
        }
    }
}
