package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.security.Principal;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("!prod & !production")
@ConditionalOnProperty(name = "openstudy.teacher-analytics.demo-access", havingValue = "true")
public class MockTeacherAnalyticsAccessProvider implements TeacherAnalyticsAccessProvider {
    @Override
    public TeacherAnalyticsScope resolve(Principal principal) {
        // Anonymous demo access is permitted ONLY in the database-free mock profile.
        if (principal != null && !"DEMO-TEACHER-001".equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This demo is restricted to the demo teacher.");
        }
        return new TeacherAnalyticsScope(new TeacherIdentityDTO("DEMO-TEACHER-001", "Dr. Priya Sharma",
                "priya.sharma@openstudy.demo", "Teacher", true), Set.of("CS-2A", "CS-2B"), Set.of("DS", "DB"));
    }
}
