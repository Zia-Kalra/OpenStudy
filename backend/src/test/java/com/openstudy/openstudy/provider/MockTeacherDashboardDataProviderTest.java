package com.openstudy.openstudy.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.openstudy.openstudy.config.DashboardProperties;
import org.junit.jupiter.api.Test;

class MockTeacherDashboardDataProviderTest {

    @Test
    void demoTeacherIdentityIsClearlyLabeled() {
        DashboardProperties properties = new DashboardProperties();
        MockTeacherDashboardDataProvider provider = new MockTeacherDashboardDataProvider(properties);

        var teacher = provider.getTeacherIdentity();

        assertThat(teacher.isDemoIdentity()).isTrue();
        assertThat(teacher.getTeacherId()).isEqualTo("DEMO-TEACHER-001");
        assertThat(teacher.getDisplayName()).isEqualTo("Dr. Priya Sharma");
    }

    @Test
    void populatedScenarioUsesDemonstrationCounts() {
        DashboardProperties properties = new DashboardProperties();
        properties.setMockScenario("populated");
        MockTeacherDashboardDataProvider provider = new MockTeacherDashboardDataProvider(properties);

        assertThat(provider.getStatistics().getTotalStudents()).isEqualTo(120);
        assertThat(provider.getRecentActivity()).isNotEmpty();
        assertThat(provider.getUpcomingDeadlines()).isNotEmpty();
    }
}
