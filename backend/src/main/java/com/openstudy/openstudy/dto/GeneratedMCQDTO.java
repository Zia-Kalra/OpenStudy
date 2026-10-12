package com.openstudy.openstudy.dto;

import java.util.List;

public class GeneratedMCQDTO {

    private final String questionText;
    private final List<String> options;
    private final String correctOption;
    private final String explanation;
    private final String difficulty;
    private final String source;

    public GeneratedMCQDTO(
            String questionText,
            List<String> options,
            String correctOption,
            String explanation,
            String difficulty,
            String source) {
        this.questionText = questionText;
        this.options = options;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.source = source;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getSource() {
        return source;
    }
}
