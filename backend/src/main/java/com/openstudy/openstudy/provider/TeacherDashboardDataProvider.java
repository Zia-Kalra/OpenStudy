package com.openstudy.openstudy.provider;

import com.openstudy.openstudy.dto.DashboardStatsDTO;
import com.openstudy.openstudy.dto.RecentActivityDTO;
import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.UpcomingDeadlineDTO;
import java.util.List;

/**
 * Data boundary for the teacher dashboard.
 * Phase 1 uses {@link MockTeacherDashboardDataProvider}.
 * A future JDBC implementation can replace it without changing the controller.
 */
public interface TeacherDashboardDataProvider {

    TeacherIdentityDTO getTeacherIdentity();

    DashboardStatsDTO getStatistics();

    List<RecentActivityDTO> getRecentActivity();

    List<UpcomingDeadlineDTO> getUpcomingDeadlines();
}
