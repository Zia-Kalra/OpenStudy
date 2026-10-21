package com.openstudy.openstudy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionBankOptionRequest(
        @NotBlank @Size(max = 4000) String text,
        boolean correct) {
}