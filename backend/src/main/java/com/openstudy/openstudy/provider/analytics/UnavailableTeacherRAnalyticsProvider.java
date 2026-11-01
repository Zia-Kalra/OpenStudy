package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO.RReport;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** No R engine or report table exists in the inspected schema. Never substitute fixtures. */
@Component
@ConditionalOnProperty(name = "openstudy.teacher-analytics.r-provider", havingValue = "none")
public class UnavailableTeacherRAnalyticsProvider implements TeacherRAnalyticsProvider {
    @Override
    public List<RReport> savedReports(TeacherAnalyticsScope scope, TeacherAnalyticsQuery query) {
        return List.of();
    }
}
