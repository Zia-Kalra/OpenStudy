package com.openstudy.openstudy.dto.analytics;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Filters narrow an authorized scope; they never grant access. */
public record TeacherAnalyticsQuery(String classId, String subjectId, String search, String band, String sort, String method,
        String assessmentSearch) {
    public TeacherAnalyticsQuery(String classId, String subjectId, String search, String band, String sort, String method) {
        this(classId, subjectId, search, band, sort, method, null);
    }
    public TeacherAnalyticsQuery {
        classId = clean(classId, "all", 64);
        subjectId = clean(subjectId, "all", 64);
        search = clean(search, "", 100);
        assessmentSearch = clean(assessmentSearch, "", 100);
        band = clean(band, "ALL", 24);
        sort = clean(sort, "SCORE", 24);
        method = clean(method, "ALL", 24);
        if (!Set.of("ALL", "STRONG", "ON_TRACK", "DEVELOPING", "NEEDS_ATTENTION", "NOT_GRADED").contains(band)
                || !Set.of("SCORE", "NAME", "SUBMISSION").contains(sort)
                || !Set.of("ALL", "DESCRIPTIVE", "T_TEST", "ANOVA", "REGRESSION").contains(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid analytics filter.");
        }
    }

    private static String clean(String value, String fallback, int limit) {
        String text = value == null || value.isBlank() ? fallback : value.strip();
        if (text.length() > limit || text.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Analytics filter is too long or invalid.");
        }
        return text;
    }

    public String getClassId() { return classId; }
    public String getSubjectId() { return subjectId; }
    public String getSearch() { return search; }
    public String getBand() { return band; }
    public String getSort() { return sort; }
    public String getMethod() { return method; }
    public String getAssessmentSearch() { return assessmentSearch; }
}
