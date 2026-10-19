package com.openstudy.openstudy.dto;

import java.util.List;

public class AssignmentEvaluationResult {

    private final double suggestedScore;
    private final double maximumScore;
    private final String feedback;
    private final List<String> strengths;
    private final List<String> areasForImprovement;
    private final String evaluationStatus;
    private final String source;
    private final boolean scoreAdjusted;
    private final String teacherAuthorityNote;

    public AssignmentEvaluationResult(
            double suggestedScore,
            double maximumScore,
            String feedback,
            List<String> strengths,
            List<String> areasForImprovement,
            String evaluationStatus,
            String source,
            boolean scoreAdjusted,
            String teacherAuthorityNote) {
        this.suggestedScore = suggestedScore;
        this.maximumScore = maximumScore;
        this.feedback = feedback;
        this.strengths = strengths;
        this.areasForImprovement = areasForImprovement;
        this.evaluationStatus = evaluationStatus;
        this.source = source;
        this.scoreAdjusted = scoreAdjusted;
        this.teacherAuthorityNote = teacherAuthorityNote;
    }

    public double getSuggestedScore() {
        return suggestedScore;
    }

    public double getMaximumScore() {
        return maximumScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public List<String> getAreasForImprovement() {
        return areasForImprovement;
    }

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public String getSource() {
        return source;
    }

    public boolean isScoreAdjusted() {
        return scoreAdjusted;
    }

    public String getTeacherAuthorityNote() {
        return teacherAuthorityNote;
    }
}
