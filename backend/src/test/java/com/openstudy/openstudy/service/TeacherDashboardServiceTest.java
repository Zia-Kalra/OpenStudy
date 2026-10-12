package com.openstudy.openstudy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.openstudy.openstudy.config.DashboardProperties;
import com.openstudy.openstudy.config.TeacherModuleProperties;
import com.openstudy.openstudy.dto.TeacherDashboardDTO;
import com.openstudy.openstudy.provider.MockTeacherDashboardDataProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TeacherDashboardServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T10:15:30Z"), ZoneId.of("UTC"));

    @Test
    void populatedDashboardExposesMockStatisticsAndActivity() {
        DashboardProperties dashboardProperties = new DashboardProperties();
        dashboardProperties.setDemoMode(true);
        dashboardProperties.setMockScenario("populated");
        TeacherDashboardService service = newService(dashboardProperties);

        TeacherDashboardDTO dashboard = service.getDashboard();

        assertThat(dashboard.getStats().getTotalQuizzes()).isEqualTo(12);
        assertThat(dashboard.getStats().getTotalAssignments()).isEqualTo(8);
        assertThat(dashboard.getStats().getTotalStudents()).isEqualTo(120);
        assertThat(dashboard.getStats().getPendingAssignmentReviews()).isEqualTo(15);
        assertThat(dashboard.isDemoData()).isTrue();
        assertThat(dashboard.isEmptyActivity()).isFalse();
        assertThat(dashboard.getRecentActivity()).hasSize(4);
        assertThat(dashboard.getQuickActions()).hasSize(4);
        assertThat(dashboard.getQuickActions()).filteredOn(action -> !action.getId().equals("view-analytics"))
                .allMatch(action -> action.getHref().contains("/teacher/coming-soon"));
        assertThat(dashboard.getQuickActions()).filteredOn(action -> action.getId().equals("view-analytics"))
                .allMatch(action -> action.getHref().equals("/teacher/analytics") && !action.isPlaceholder() && action.getOwner().equals("Member 5"));
        assertThat(dashboard.getGreeting()).contains("Dr. Priya Sharma");
    }

    @Test
    void emptyScenarioShowsEmptyStatesAndZeroCounts() {
        DashboardProperties dashboardProperties = new DashboardProperties();
        dashboardProperties.setMockScenario("empty");
        TeacherDashboardService service = newService(dashboardProperties);

        TeacherDashboardDTO dashboard = service.getDashboard();

        assertThat(dashboard.getStats().isNoQuizzes()).isTrue();
        assertThat(dashboard.getStats().isNoAssignments()).isTrue();
        assertThat(dashboard.getStats().isNoPendingReviews()).isTrue();
        assertThat(dashboard.isEmptyActivity()).isTrue();
        assertThat(dashboard.isEmptyDeadlines()).isTrue();
        assertThat(dashboard.getRecentActivity()).isEmpty();
    }

    private TeacherDashboardService newService(DashboardProperties dashboardProperties) {
        MockTeacherDashboardDataProvider provider =
                new MockTeacherDashboardDataProvider(dashboardProperties, clock);
        return new TeacherDashboardService(provider, dashboardProperties, new TeacherModuleProperties(), clock);
    }
}
