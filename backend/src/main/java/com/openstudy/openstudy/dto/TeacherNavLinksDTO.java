package com.openstudy.openstudy.dto;

public class TeacherNavLinksDTO {

    private final String dashboard;
    private final String questionBank;
    private final String quizzes;
    private final String assignments;
    private final String submissions;
    private final String analytics;
    private final String profile;
    private final String logout;

    public TeacherNavLinksDTO(
            String dashboard,
            String questionBank,
            String quizzes,
            String assignments,
            String submissions,
            String analytics,
            String profile,
            String logout) {
        this.dashboard = dashboard;
        this.questionBank = questionBank;
        this.quizzes = quizzes;
        this.assignments = assignments;
        this.submissions = submissions;
        this.analytics = analytics;
        this.profile = profile;
        this.logout = logout;
    }

    public String getDashboard() {
        return dashboard;
    }

    public String getQuestionBank() {
        return questionBank;
    }

    public String getQuizzes() {
        return quizzes;
    }

    public String getAssignments() {
        return assignments;
    }

    public String getSubmissions() {
        return submissions;
    }

    public String getAnalytics() {
        return analytics;
    }

    public String getProfile() {
        return profile;
    }

    public String getLogout() {
        return logout;
    }
}
