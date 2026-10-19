package com.openstudy.openstudy.dto;

public class DashboardStatsDTO {

    private final int totalQuizzes;
    private final int totalAssignments;
    private final int totalStudents;
    private final int pendingAssignmentReviews;

    public DashboardStatsDTO(
            int totalQuizzes,
            int totalAssignments,
            int totalStudents,
            int pendingAssignmentReviews) {
        this.totalQuizzes = totalQuizzes;
        this.totalAssignments = totalAssignments;
        this.totalStudents = totalStudents;
        this.pendingAssignmentReviews = pendingAssignmentReviews;
    }

    public int getTotalQuizzes() {
        return totalQuizzes;
    }

    public int getTotalAssignments() {
        return totalAssignments;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getPendingAssignmentReviews() {
        return pendingAssignmentReviews;
    }

    public boolean isNoQuizzes() {
        return totalQuizzes == 0;
    }

    public boolean isNoAssignments() {
        return totalAssignments == 0;
    }

    public boolean isNoPendingReviews() {
        return pendingAssignmentReviews == 0;
    }
}
