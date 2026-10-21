package com.openstudy.openstudy.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuestionBankQuestionResponse(
        long bankQuestionId,
        String questionText,
        String questionType,
        BigDecimal points,
        String difficulty,
        String source,
        String approvalStatus,
        List<Option> options) {

    public record Option(long bankOptionId, String text, boolean correct, int position) {
    }
}