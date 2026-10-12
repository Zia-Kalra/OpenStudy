package com.openstudy.openstudy.provider;

import com.openstudy.openstudy.config.DashboardProperties;
import com.openstudy.openstudy.dto.DashboardStatsDTO;
import com.openstudy.openstudy.dto.RecentActivityDTO;
import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.UpcomingDeadlineDTO;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "openstudy.dashboard.provider", havingValue = "mock", matchIfMissing = true)
public class MockTeacherDashboardDataProvider implements TeacherDashboardDataProvider {

    static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final DashboardProperties properties;
    private final Clock clock;

    @Autowired
    public MockTeacherDashboardDataProvider(DashboardProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    public MockTeacherDashboardDataProvider(DashboardProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public TeacherIdentityDTO getTeacherIdentity() {
        return new TeacherIdentityDTO(
                "DEMO-TEACHER-001",
                "Dr. Priya Sharma",
                "priya.sharma@openstudy.demo",
                "Teacher",
                true);
    }

    @Override
    public DashboardStatsDTO getStatistics() {
        if (properties.isEmptyScenario()) {
            return new DashboardStatsDTO(0, 0, 0, 0);
        }
        return new DashboardStatsDTO(12, 8, 120, 15);
    }

    @Override
    public List<RecentActivityDTO> getRecentActivity() {
        if (properties.isEmptyScenario()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return List.of(
                activity("act-1", "QUIZ_PUBLISHED", "Quiz published",
                        "Midterm MCQ quiz for Data Structures was published.", now.minusHours(3)),
                activity("act-2", "ASSIGNMENT_CREATED", "Assignment created",
                        "Week 4 written assignment on operating systems was created.", now.minusHours(8)),
                activity("act-3", "ASSIGNMENT_SUBMITTED", "Assignment submitted",
                        "18 students submitted the Week 3 database assignment.", now.minusDays(1)),
                activity("act-4", "EVALUATION_COMPLETED", "Evaluation completed",
                        "Suggested scores were reviewed for 6 short-answer submissions.", now.minusDays(2)));
    }

    @Override
    public List<UpcomingDeadlineDTO> getUpcomingDeadlines() {
        if (properties.isEmptyScenario()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return List.of(
                deadline("quiz-1", "QUIZ", "Unit 2 Algorithms Quiz", now.plusDays(2).withHour(17).withMinute(0), "CS201"),
                deadline("asg-1", "ASSIGNMENT", "SQL Query Worksheet", now.plusDays(4).withHour(23).withMinute(59), "CS204"),
                deadline("asg-2", "ASSIGNMENT", "Process Scheduling Reflection", now.plusDays(7).withHour(18).withMinute(0), "CS203"));
    }

    private RecentActivityDTO activity(
            String id, String type, String title, String description, LocalDateTime occurredAt) {
        return new RecentActivityDTO(id, type, title, description, occurredAt, occurredAt.format(DISPLAY));
    }

    private UpcomingDeadlineDTO deadline(
            String id, String type, String title, LocalDateTime dueAt, String course) {
        return new UpcomingDeadlineDTO(id, type, title, dueAt, dueAt.format(DISPLAY), course);
    }
}
