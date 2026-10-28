package com.openstudy.openstudy.dto.analytics;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Validated analytics filters. The only way to build one from request input is {@link #of},
 * so every value reaching SQL has already been parsed into a safe type.
 *
 * @param subjectId optional subject (subjects.subject_id)
 * @param type      null (all), "QUIZ" or "ASSIGNMENT"
 * @param from      inclusive start date, optional
 * @param to        inclusive end date, optional
 */
public record AnalyticsFilter(Long subjectId, String type, LocalDate from, LocalDate to) {

    private static final long MAX_RANGE_DAYS = 366L * 5;

    public static AnalyticsFilter none() {
        return new AnalyticsFilter(null, null, null, null);
    }

    public static AnalyticsFilter of(Long subjectId, String type, LocalDate from, LocalDate to) {
        if (subjectId != null && subjectId <= 0) {
            throw new IllegalArgumentException("subjectId must be a positive number.");
        }
        String normalizedType = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type.trim())) {
            normalizedType = type.trim().toUpperCase(Locale.ROOT);
            if (!normalizedType.equals("QUIZ") && !normalizedType.equals("ASSIGNMENT")) {
                throw new IllegalArgumentException("type must be QUIZ, ASSIGNMENT or ALL.");
            }
        }
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("'from' must not be after 'to'.");
            }
            if (to.toEpochDay() - from.toEpochDay() > MAX_RANGE_DAYS) {
                throw new IllegalArgumentException("The date range must not exceed 5 years.");
            }
        }
        return new AnalyticsFilter(subjectId, normalizedType, from, to);
    }

    /** Stable text used as a cache key. */
    public String cacheKey() {
        return subjectId + "|" + type + "|" + from + "|" + to;
    }
}
