package com.openstudy.openstudy.service;

import com.openstudy.openstudy.config.DashboardProperties;
import com.openstudy.openstudy.config.TeacherModuleProperties;
import com.openstudy.openstudy.dto.QuickActionDTO;
import com.openstudy.openstudy.dto.TeacherDashboardDTO;
import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.TeacherNavLinksDTO;
import com.openstudy.openstudy.provider.TeacherDashboardDataProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeacherDashboardService {

    private static final DateTimeFormatter DATE_DISPLAY =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private final TeacherDashboardDataProvider dataProvider;
    private final DashboardProperties dashboardProperties;
    private final TeacherModuleProperties moduleProperties;
    private final Clock clock;

    @Autowired
    public TeacherDashboardService(
            TeacherDashboardDataProvider dataProvider,
            DashboardProperties dashboardProperties,
            TeacherModuleProperties moduleProperties) {
        this(dataProvider, dashboardProperties, moduleProperties, Clock.systemDefaultZone());
    }

    public TeacherDashboardService(
            TeacherDashboardDataProvider dataProvider,
            DashboardProperties dashboardProperties,
            TeacherModuleProperties moduleProperties,
            Clock clock) {
        this.dataProvider = dataProvider;
        this.dashboardProperties = dashboardProperties;
        this.moduleProperties = moduleProperties;
        this.clock = clock;
    }

    public TeacherDashboardDTO getDashboard() {
        TeacherIdentityDTO teacher = dataProvider.getTeacherIdentity();
        var stats = dataProvider.getStatistics();
        var activity = dataProvider.getRecentActivity();
        var deadlines = dataProvider.getUpcomingDeadlines();
        return new TeacherDashboardDTO(
                teacher,
                greeting(teacher.getDisplayName()),
                LocalDate.now(clock).format(DATE_DISPLAY),
                "Teacher Dashboard",
                "Review class activity, upcoming work, and jump into the modules owned by the rest of the team.",
                stats,
                activity,
                deadlines,
                quickActions(),
                dashboardProperties.isDemoMode() || teacher.isDemoIdentity(),
                activity.isEmpty(),
                deadlines.isEmpty());
    }

    public TeacherNavLinksDTO getNavLinks() {
        return new TeacherNavLinksDTO(
                moduleProperties.getDashboardPath(),
                moduleProperties.getQuestionBankPath(),
                moduleProperties.getQuizzesPath(),
                moduleProperties.getAssignmentsPath(),
                moduleProperties.getSubmissionsPath(),
                moduleProperties.getAnalyticsPath(),
                moduleProperties.getProfilePath(),
                moduleProperties.getLogoutPath());
    }

    public TeacherIdentityDTO getTeacherIdentity() {
        return dataProvider.getTeacherIdentity();
    }

    private List<QuickActionDTO> quickActions() {
        return List.of(
                new QuickActionDTO(
                        "create-quiz",
                        "Create Quiz",
                        "Opens Member 2's quiz module when it is integrated.",
                        moduleProperties.getQuizzesPath(),
                        "Member 2",
                        isPlaceholder(moduleProperties.getQuizzesPath())),
                new QuickActionDTO(
                        "create-assignment",
                        "Create Assignment",
                        "Create and publish assignments for enrolled students.",
                        moduleProperties.getAssignmentsPath(),
                        "Member 3",
                        isPlaceholder(moduleProperties.getAssignmentsPath())),
                new QuickActionDTO(
                        "view-submissions",
                        "View Submissions",
                        "Review student submissions and AI evaluations.",
                        moduleProperties.getSubmissionsPath(),
                        "Member 3",
                        isPlaceholder(moduleProperties.getSubmissionsPath())),
                new QuickActionDTO(
                        "view-analytics",
                        "View Analytics",
                        "Compare assigned students, assessments, and class performance.",
                        moduleProperties.getAnalyticsPath(),
                        "Member 5",
                        isPlaceholder(moduleProperties.getAnalyticsPath())));
    }

    private boolean isPlaceholder(String path) {
        return path != null && path.contains("/teacher/coming-soon");
    }

    private String greeting(String name) {
        int hour = java.time.LocalTime.now(clock).getHour();
        String partOfDay;
        if (hour < 12) {
            partOfDay = "Good morning";
        } else if (hour < 17) {
            partOfDay = "Good afternoon";
        } else {
            partOfDay = "Good evening";
        }
        return partOfDay + ", " + name;
    }
}
