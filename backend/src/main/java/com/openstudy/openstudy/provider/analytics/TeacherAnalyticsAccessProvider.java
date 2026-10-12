package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.security.Principal;

/** Member 1 adapter must verify TEACHER role and load assignments server-side from trusted identity. */
public interface TeacherAnalyticsAccessProvider {
    TeacherAnalyticsScope resolve(Principal principal);
}
