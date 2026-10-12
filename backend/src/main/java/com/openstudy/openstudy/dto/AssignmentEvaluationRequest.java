package com.openstudy.openstudy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;

public class AssignmentEvaluationRequest {

    @NotBlank
    private String questionText;

    @NotBlank
    private String studentAnswer;

    @NotBlank
    private String modelAnswer;

    private List<RubricCriterionDTO> rubricCriteria = new ArrayList<>();

    @Positive
    private double maximumMarks;

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public String getModelAnswer() {
        return modelAnswer;
    }

    public void setModelAnswer(String modelAnswer) {
        this.modelAnswer = modelAnswer;
    }

    public List<RubricCriterionDTO> getRubricCriteria() {
        return rubricCriteria;
    }

    public void setRubricCriteria(List<RubricCriterionDTO> rubricCriteria) {
        this.rubricCriteria = rubricCriteria != null ? rubricCriteria : new ArrayList<>();
    }

    public double getMaximumMarks() {
        return maximumMarks;
    }

    public void setMaximumMarks(double maximumMarks) {
        this.maximumMarks = maximumMarks;
    }
}
