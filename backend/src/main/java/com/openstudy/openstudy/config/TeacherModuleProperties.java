package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.modules")
public class TeacherModuleProperties {

    private String dashboardPath = "/teacher/dashboard";
    private String questionBankPath = "/teacher/coming-soon?module=question-bank";
    private String quizzesPath = "/teacher/coming-soon?module=quizzes";
    private String assignmentsPath = "/teacher/assignments";
    private String submissionsPath = "/teacher/submissions";
    private String analyticsPath = "/teacher/coming-soon?module=analytics";
    private String profilePath = "/teacher/coming-soon?module=profile";
    private String logoutPath = "/logout";

    public String getDashboardPath() {
        return dashboardPath;
    }

    public void setDashboardPath(String dashboardPath) {
        this.dashboardPath = internalPath(dashboardPath);
    }

    public String getQuestionBankPath() {
        return questionBankPath;
    }

    public void setQuestionBankPath(String questionBankPath) {
        this.questionBankPath = internalPath(questionBankPath);
    }

    public String getQuizzesPath() {
        return quizzesPath;
    }

    public void setQuizzesPath(String quizzesPath) {
        this.quizzesPath = internalPath(quizzesPath);
    }

    public String getAssignmentsPath() {
        return assignmentsPath;
    }

    public void setAssignmentsPath(String assignmentsPath) {
        this.assignmentsPath = internalPath(assignmentsPath);
    }

    public String getSubmissionsPath() {
        return submissionsPath;
    }

    public void setSubmissionsPath(String submissionsPath) {
        this.submissionsPath = internalPath(submissionsPath);
    }

    public String getAnalyticsPath() {
        return analyticsPath;
    }

    public void setAnalyticsPath(String analyticsPath) {
        this.analyticsPath = internalPath(analyticsPath);
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = internalPath(profilePath);
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public void setLogoutPath(String logoutPath) {
        this.logoutPath = internalPath(logoutPath);
    }
    private String internalPath(String value) {
        if (value == null || !value.startsWith("/") || value.startsWith("//")
                || value.indexOf(92) >= 0 || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Teacher module paths must be application-relative paths starting with a single slash.");
        }
        return value;
    }
}
