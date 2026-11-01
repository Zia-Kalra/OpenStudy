package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO.RReport;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.util.List;

/** Adapter to the team's existing R calculations/storage; never invoke a shell from HTTP input. */
public interface TeacherRAnalyticsProvider {
    List<RReport> savedReports(TeacherAnalyticsScope scope, TeacherAnalyticsQuery query);
}
