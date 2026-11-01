package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.Snapshot;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;

public interface TeacherAnalyticsDataProvider {
    /** Adapters must bind teacher ID and intersect trusted assignments before reading results. */
    Snapshot load(TeacherAnalyticsScope scope);
}
