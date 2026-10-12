package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.security.Principal;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnavailableTeacherAnalyticsAccessProvider implements TeacherAnalyticsAccessProvider {
    @Override
    public TeacherAnalyticsScope resolve(Principal principal) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Teacher analytics requires the authenticated teacher access adapter. Use mock for the local demo.");
    }
}
