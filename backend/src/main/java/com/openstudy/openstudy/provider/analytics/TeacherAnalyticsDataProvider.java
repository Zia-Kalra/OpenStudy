package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.Snapshot;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;

public interface TeacherAnalyticsDataProvider {
    /** Future JDBC queries must bind teacher ID and intersect class/subject assignments before reading any results. */
    Snapshot load(TeacherAnalyticsScope scope);
}
