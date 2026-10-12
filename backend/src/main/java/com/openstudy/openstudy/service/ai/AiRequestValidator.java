package com.openstudy.openstudy.service.ai;

import com.openstudy.openstudy.dto.AssignmentEvaluationRequest;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Validation at the shared service boundary, including direct Java callers. */
final class AiRequestValidator {
    static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private AiRequestValidator() { }

    static void validate(MCQGenerationRequest request) {
        require(request != null, "MCQ request is required.");
        text(request.getSubject(), "Subject", 200, true);
        text(request.getTopic(), "Topic", 500, true);
        difficulty(request.getDifficulty());
        require(request.getNumberOfQuestions() != null && request.getNumberOfQuestions() >= 1
                && request.getNumberOfQuestions() <= 20, "Question count must be between 1 and 20.");
        text(request.getInstructions(), "Instructions", 2000, false);
    }

    static void validate(AssignmentEvaluationRequest request) {
        require(request != null, "Evaluation request is required.");
        text(request.getQuestionText(), "Question", 10000, true);
        text(request.getStudentAnswer(), "Student answer", 20000, true);
        text(request.getModelAnswer(), "Model answer", 20000, true);
        maximum(request.getMaximumMarks());
        var criteria = request.getRubricCriteria();
        require(criteria != null && criteria.size() <= 20, "Rubric must contain at most 20 criteria.");
        Set<String> names = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var criterion : criteria) {
            require(criterion != null, "Rubric criterion is required.");
            text(criterion.getName(), "Rubric name", 200, true);
            text(criterion.getDescription(), "Rubric description", 2000, true);
            require(names.add(criterion.getName().strip().toUpperCase(Locale.ROOT)),
                    "Rubric names must be distinct.");
            maximum(criterion.getMaxMarks());
            total = total.add(BigDecimal.valueOf(criterion.getMaxMarks()));
        }
        require(total.compareTo(BigDecimal.valueOf(request.getMaximumMarks())) <= 0,
                "Rubric marks must not exceed the assignment maximum.");
    }

    static String difficulty(String value) {
        text(value, "Difficulty", 20, true);
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        require(DIFFICULTIES.contains(normalized), "Difficulty must be EASY, MEDIUM, or HARD.");
        return normalized;
    }

    static void maximum(double value) {
        require(Double.isFinite(value) && value > 0, "Maximum marks must be finite and greater than zero.");
    }

    static void text(String value, String name, int limit, boolean required) {
        require(!required || (value != null && !value.isBlank()), name + " is required.");
        require(value == null || value.length() <= limit, name + " exceeds the allowed length.");
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AiServiceException(AiErrorCode.VALIDATION_FAILED, message);
        }
    }
}
