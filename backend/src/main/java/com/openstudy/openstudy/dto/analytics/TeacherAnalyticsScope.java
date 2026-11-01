package com.openstudy.openstudy.dto.analytics;

import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import java.util.Set;

/** Trusted server-side scope. Never bind this type from HTTP parameters or bodies. */
public record TeacherAnalyticsScope(TeacherIdentityDTO teacher, Set<String> classIds, Set<String> subjectIds) {
    public TeacherAnalyticsScope {
        if (teacher == null || !"Teacher".equalsIgnoreCase(teacher.getRole())
                || teacher.getTeacherId() == null || teacher.getTeacherId().isBlank()) {
            throw new IllegalArgumentException("A verified teacher identity is required.");
        }
        classIds = Set.copyOf(classIds);
        subjectIds = Set.copyOf(subjectIds);
    }
}
