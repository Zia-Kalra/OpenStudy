package com.openstudy.openstudy.provider.analytics;

/** Read-only projection of the integrated Teacher Submissions review permission. */
public final class TeacherAnalyticsVisibility {
    private TeacherAnalyticsVisibility() { }

    public static final String REVIEWER_JOIN = """
             JOIN users analytics_reviewer ON analytics_reviewer.user_id = ?
               AND analytics_reviewer.role = 'TEACHER' AND analytics_reviewer.account_status = 'ACTIVE'
            """;

    // Editing still requires ownership. Review access additionally covers existing submitted work,
    // as in AssignmentController.teacherSubmissions; unpublished foreign assignments stay private.
    public static final String ASSIGNMENT_REVIEW = """
            (a.created_by = analytics_reviewer.user_id OR
             (a.status IN ('PUBLISHED', 'CLOSED') AND EXISTS
              (SELECT 1 FROM assignment_submissions review_submission
               WHERE review_submission.assignment_id = a.assignment_id
                 AND review_submission.status IN ('SUBMITTED', 'GRADED'))))
            """;

    public static final String SUBJECTS = """
            SELECT s.subject_id, s.subject_name FROM subjects s
            """ + REVIEWER_JOIN + """
            WHERE EXISTS (SELECT 1 FROM subject_teachers st
                          WHERE st.subject_id=s.subject_id AND st.teacher_id=analytics_reviewer.user_id)
               OR EXISTS (SELECT 1 FROM assignments a WHERE a.subject_id=s.subject_id AND
            """ + ASSIGNMENT_REVIEW + """
               )
               OR EXISTS (SELECT 1 FROM quizzes q
                          WHERE q.subject_id=s.subject_id AND q.created_by=analytics_reviewer.user_id)
            ORDER BY s.subject_name, s.subject_id
            """;
}
