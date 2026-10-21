package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnavailableTeacherAnalyticsAccessProvider implements TeacherAnalyticsAccessProvider {
    @Override
    public TeacherAnalyticsScope resolve(Principal principal) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Teacher analytics requires an authenticated teacher access adapter. Enable openstudy.teacher-analytics.demo-access for local demo data, or provide a real adapter.");
    }
}
