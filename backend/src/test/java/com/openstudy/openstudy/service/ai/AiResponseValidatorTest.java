package com.openstudy.openstudy.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openstudy.openstudy.dto.AssignmentEvaluationRequest;
import com.openstudy.openstudy.dto.GeneratedMCQDTO;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiResponseValidatorTest {

    private final AiResponseValidator validator = new AiResponseValidator();

    @Test
    void rejectsMcqWhenCorrectOptionIsMissingFromChoices() {
        MCQGenerationRequest request = request();
        GeneratedMCQDTO invalid = new GeneratedMCQDTO(
                "What is a stack?",
                List.of("LIFO", "FIFO", "Tree", "Graph"),
                "Missing",
                "explanation",
                "EASY",
                AiResponseValidator.SOURCE_GROQ);

        assertThatThrownBy(() -> validator.validateMcqs(request, List.of(invalid)))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.VALIDATION_FAILED);
    }

    @Test
    void clampsSuggestedScoreToMaximumMarks() {
        AssignmentEvaluationRequest request = new AssignmentEvaluationRequest();
        request.setMaximumMarks(10);
        request.setQuestionText("Q");
        request.setStudentAnswer("A");
        request.setModelAnswer("M");

        var result = validator.validateEvaluation(
                request,
                15,
                "Clear writing.",
                List.of("Structure"),
                List.of("Detail"),
                AiResponseValidator.SOURCE_GROQ);

        assertThat(result.getSuggestedScore()).isEqualTo(10.0);
        assertThat(result.isScoreAdjusted()).isTrue();
        assertThat(result.getEvaluationStatus()).isEqualTo(AiResponseValidator.STATUS_SCORE_ADJUSTED);
        assertThat(result.getTeacherAuthorityNote()).contains("final grading authority");
    }

    @Test
    void rejectsEmptyFeedback() {
        AssignmentEvaluationRequest request = new AssignmentEvaluationRequest();
        request.setMaximumMarks(10);
        request.setQuestionText("Q");
        request.setStudentAnswer("A");
        request.setModelAnswer("M");

        assertThatThrownBy(() -> validator.validateEvaluation(
                        request, 5, "  ", List.of(), List.of(), AiResponseValidator.SOURCE_GROQ))
                .isInstanceOf(AiServiceException.class)
                .extracting(ex -> ((AiServiceException) ex).getErrorCode())
                .isEqualTo(AiErrorCode.VALIDATION_FAILED);
    }

    private MCQGenerationRequest request() {
        MCQGenerationRequest request = new MCQGenerationRequest();
        request.setSubject("CS");
        request.setTopic("Stacks");
        request.setDifficulty("EASY");
        request.setNumberOfQuestions(1);
        return request;
    }

    private GeneratedMCQDTO question(List<String> options, String answer) {
        return new GeneratedMCQDTO("Question", options, answer, "Explanation", "EASY", "GROQ");
    }

    @Test
    void requiresExactQuestionCount() {
        var request = request();
        var question = question(List.of("A", "B", "C", "D"), "A");
        request.setNumberOfQuestions(2);
        assertThatThrownBy(() -> validator.validateMcqs(request, List.of(question)))
                .isInstanceOf(AiServiceException.class);
        request.setNumberOfQuestions(1);
        assertThatThrownBy(() -> validator.validateMcqs(request, List.of(question, question)))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void requiresFourNonblankDistinctOptions() {
        for (var options : List.of(List.of("A", "B"), List.of("A", "B", "C", "D", "E"),
                List.of("A", " a ", "C", "D"), List.of("A", " ", "C", "D"))) {
            assertThatThrownBy(() -> validator.validateMcqs(request(), List.of(question(options, "A"))))
                    .isInstanceOf(AiServiceException.class);
        }
        assertThatThrownBy(() -> validator.validateMcqs(request(), List.of(question(
                java.util.Arrays.asList("A", null, "C", "D"), "A"))))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void returnsCanonicalAnswerAndRejectsInvalidDifficulty() {
        var result = validator.validateMcqs(request(), List.of(question(List.of(" Linear ", "Constant", "Log", "Quadratic"), "linear")));
        assertThat(result.getFirst().getCorrectOption()).isEqualTo("Linear");
        assertThat(result.getFirst().getOptions()).contains("Linear");
        assertThatThrownBy(() -> validator.validateMcqs(request(), List.of(new GeneratedMCQDTO(
                "Q", List.of("A", "B", "C", "D"), "A", "E", "UNKNOWN", "GROQ"))))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    void rejectsNonfiniteSuggestedScores() {
        for (double score : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            assertThatThrownBy(() -> validator.validateEvaluation(AiRequestValidationTest.evaluation(), score,
                    "Feedback", List.of(), List.of(), "GROQ")).isInstanceOf(AiServiceException.class);
        }
    }

    @Test
    void roundedScoreNeverExceedsOriginalMaximum() {
        var request = AiRequestValidationTest.evaluation();
        request.setMaximumMarks(1.006);
        var result = validator.validateEvaluation(request, 1.006, "Feedback", List.of(), List.of(), "GROQ");
        assertThat(result.getSuggestedScore()).isEqualTo(1.00);
        assertThat(result.getMaximumScore()).isEqualTo(1.006);
        assertThat(result.isScoreAdjusted()).isTrue();
        request.setMaximumMarks(0.004);
        assertThat(validator.validateEvaluation(request, 0.004, "F", List.of(), List.of(), "GROQ")
                .getSuggestedScore()).isZero();
    }

    @Test
    void roundsHalfUpWithoutOverflowAndClampsNegativeScores() {
        var request = AiRequestValidationTest.evaluation();
        assertThat(validator.validateEvaluation(request, 1.005, "F", List.of(), List.of(), "GROQ")
                .getSuggestedScore()).isEqualTo(1.01);
        var negative = validator.validateEvaluation(request, -1, "F", List.of(), List.of(), "GROQ");
        assertThat(negative.getSuggestedScore()).isZero();
        assertThat(negative.isScoreAdjusted()).isTrue();
        request.setMaximumMarks(Double.MAX_VALUE);
        assertThat(validator.validateEvaluation(request, Double.MAX_VALUE, "F", List.of(), List.of(), "GROQ")
                .getSuggestedScore()).isFinite().isLessThanOrEqualTo(request.getMaximumMarks());
    }
}
