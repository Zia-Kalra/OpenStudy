package com.openstudy.openstudy.service.analytics;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Identity boundary for analytics. Member 1's authentication replaces the implementation (or just
 * keeps storing a {@code User} in the session under {@code openstudy.analytics.session-attribute});
 * the analytics code never reads a user id from request parameters.
 */
public interface AnalyticsUserContext {

    enum Role { STUDENT, TEACHER }

    /** @return the authenticated user id acting in the required role
     *  @throws AnalyticsAccessException 401 if nobody is signed in, 403 if the role does not match */
    long requireUserId(HttpServletRequest request, Role required);
}
