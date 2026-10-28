package com.openstudy.openstudy.service.analytics;

import java.time.LocalDateTime;

/** One line of the CSV handed to R. studentKey is a per-request pseudonym, never a database id. */
public record DatasetRow(String studentKey, String assessmentType, String subjectName, String assessmentTitle,
                         double percentage, LocalDateTime completedAt) {
}
