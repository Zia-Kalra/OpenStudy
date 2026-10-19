package com.openstudy.openstudy.dto;

import java.util.List;

public class TeacherDashboardDTO {

    private final TeacherIdentityDTO teacher;
    private final String greeting;
    private final String currentDate;
    private final String heading;
    private final String welcomeMessage;
    private final DashboardStatsDTO stats;
    private final List<RecentActivityDTO> recentActivity;
    private final List<UpcomingDeadlineDTO> upcomingDeadlines;
    private final List<QuickActionDTO> quickActions;
    private final boolean demoData;
    private final boolean emptyActivity;
    private final boolean emptyDeadlines;

    public TeacherDashboardDTO(
            TeacherIdentityDTO teacher,
            String greeting,
            String currentDate,
            String heading,
            String welcomeMessage,
            DashboardStatsDTO stats,
            List<RecentActivityDTO> recentActivity,
            List<UpcomingDeadlineDTO> upcomingDeadlines,
            List<QuickActionDTO> quickActions,
            boolean demoData,
            boolean emptyActivity,
            boolean emptyDeadlines) {
        this.teacher = teacher;
        this.greeting = greeting;
        this.currentDate = currentDate;
        this.heading = heading;
        this.welcomeMessage = welcomeMessage;
        this.stats = stats;
        this.recentActivity = recentActivity;
        this.upcomingDeadlines = upcomingDeadlines;
        this.quickActions = quickActions;
        this.demoData = demoData;
        this.emptyActivity = emptyActivity;
        this.emptyDeadlines = emptyDeadlines;
    }

    public TeacherIdentityDTO getTeacher() {
        return teacher;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public String getHeading() {
        return heading;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public DashboardStatsDTO getStats() {
        return stats;
    }

    public List<RecentActivityDTO> getRecentActivity() {
        return recentActivity;
    }

    public List<UpcomingDeadlineDTO> getUpcomingDeadlines() {
        return upcomingDeadlines;
    }

    public List<QuickActionDTO> getQuickActions() {
        return quickActions;
    }

    public boolean isDemoData() {
        return demoData;
    }

    public boolean isEmptyActivity() {
        return emptyActivity;
    }

    public boolean isEmptyDeadlines() {
        return emptyDeadlines;
    }
}
