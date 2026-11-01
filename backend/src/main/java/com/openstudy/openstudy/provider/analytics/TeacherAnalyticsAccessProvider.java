package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;

/** Member 1 adapter must verify TEACHER role and load assignments server-side from trusted identity. */
public interface TeacherAnalyticsAccessProvider {
    TeacherAnalyticsScope resolve(Principal principal);

    default TeacherAnalyticsScope resolveRequest(HttpServletRequest request) {
        return resolve(request.getUserPrincipal());
    }
}
