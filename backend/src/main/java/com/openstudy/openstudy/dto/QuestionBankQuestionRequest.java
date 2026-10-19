package com.openstudy.openstudy.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionBankQuestionRequest(
        @NotBlank @Size(max = 4000) String questionText,
        @NotNull QuestionType questionType,
        @NotNull @DecimalMin("0.01") BigDecimal points,
        @NotNull Difficulty difficulty,
        @NotEmpty @Size(min = 2, max = 6) List<@Valid QuestionBankOptionRequest> options) {

    public enum QuestionType {
        SINGLE_CHOICE,
        MULTIPLE_CHOICE
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }
}