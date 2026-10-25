package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** SELECT-only adapter for the inspected Aiven schema. Every query binds the verified teacher ID. */
@Component
@Profile("!mock")
@ConditionalOnProperty(name = "openstudy.teacher-analytics.provider", havingValue = "jdbc")
public class JdbcTeacherAnalyticsDataProvider implements TeacherAnalyticsDataProvider {
    private final JdbcTemplate jdbc;

    public JdbcTeacherAnalyticsDataProvider(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(readOnly = true)
    public Snapshot load(TeacherAnalyticsScope scope) {
        if (scope.teacher().isDemoIdentity()) { throw new IllegalArgumentException("A database teacher is required."); }
        long teacherId = Long.parseLong(scope.teacher().getTeacherId());
        if (!Integer.valueOf(1).equals(jdbc.queryForObject("""
                SELECT COUNT(*) FROM users WHERE user_id=? AND role='TEACHER' AND account_status='ACTIVE'
                """, Integer.class, teacherId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An active teacher account is required.");
        }
        var notices = new ArrayList<String>();
        notices.add("Teacher Submissions review access includes existing submitted assignment work. Quiz access remains limited to your own quizzes; draft work is excluded.");
        notices.add("Class/cohort relationships are not stored. Results include submitted assignments you can review and quizzes you created; no class membership is inferred.");
        var subjects = jdbc.query(TeacherAnalyticsVisibility.SUBJECTS,
                (row, index) -> new Subject(row.getString("subject_id"), row.getString("subject_name")), teacherId)
                .stream().filter(subject -> scope.subjectIds().contains(subject.id())).toList();
        Set<String> subjectIds = new HashSet<>();
        subjects.forEach(subject -> subjectIds.add(subject.id()));
        if (subjects.isEmpty()) {
            notices.add("No linked subjects or reviewable assessments are available for your teacher account (user ID " + teacherId
                    + "). Only authorized submitted work and explicitly linked subjects are included.");
            return new Snapshot(List.of(), subjects, List.of(), List.of(), List.of(), List.of(), false, notices);
        }

        var studentsById = new LinkedHashMap<String, Student>();
        var enrollmentTimes = new HashMap<String, LocalDateTime>();
        for (var row : jdbc.queryForList("""
                SELECT u.user_id, u.full_name, e.subject_id, e.enrolled_at
                FROM subject_enrollments e
                JOIN users u ON u.user_id = e.student_user_id
                WHERE e.enrollment_status = 'ENROLLED' AND u.role = 'STUDENT'
                  AND (EXISTS (SELECT 1 FROM subject_teachers st WHERE st.subject_id=e.subject_id AND st.teacher_id=?)
                    OR EXISTS (SELECT 1 FROM assignments a WHERE a.subject_id=e.subject_id AND a.created_by=?)
                    OR EXISTS (SELECT 1 FROM quizzes q WHERE q.subject_id=e.subject_id AND q.created_by=?))
                ORDER BY u.full_name, u.user_id, e.subject_id
                """, teacherId, teacherId, teacherId)) {
            String subjectId = text(row, "subject_id");
            if (!subjectIds.contains(subjectId)) { continue; }
            String id = text(row, "user_id");
            var previous = studentsById.get(id);
            var enrolled = new HashSet<>(previous == null ? Set.<String>of() : previous.subjectIds());
            enrolled.add(subjectId);
            studentsById.put(id, new Student(id, "User " + id, text(row, "full_name"), "", enrolled));
            enrollmentTimes.put(key(id, subjectId), time(row.get("enrolled_at")));
        }
        if (studentsById.isEmpty()) {
            notices.add("No active enrollment roster is stored for these subjects. Recorded participants in reviewable assessments are still shown; expected-work counts and submission rates remain unavailable without enrollment links.");
        }

        var assessments = new LinkedHashMap<String, Assessment>();
        var deadlines = new HashMap<String, LocalDateTime>();
        var ownedAssessments = new HashSet<String>();
        for (var row : jdbc.queryForList("""
                SELECT a.assignment_id, a.subject_id, a.title, a.max_score,
                       DATE(COALESCE(a.due_at, a.created_at)) AS assessment_date, a.due_at AS deadline, a.created_by AS owner_id
                FROM assignments a
                """ + TeacherAnalyticsVisibility.REVIEWER_JOIN + " WHERE "
                + TeacherAnalyticsVisibility.ASSIGNMENT_REVIEW + """
                AND a.status IN ('PUBLISHED', 'CLOSED')
                ORDER BY a.assignment_id
                """, teacherId)) {
            if (((Number) row.get("owner_id")).longValue() == teacherId) { ownedAssessments.add("A-" + text(row, "assignment_id")); }
            addAssessment(row, "A-", "assignment_id", "ASSIGNMENT", subjectIds, assessments, deadlines, notices);
        }
        for (var row : jdbc.queryForList("""
                SELECT q.quiz_id, q.subject_id, q.title, COALESCE(SUM(b.points), 0) AS max_score,
                       DATE(COALESCE(q.available_from, q.created_at)) AS assessment_date,
                       q.available_until AS deadline, q.created_by AS owner_id
                FROM quizzes q
                LEFT JOIN quiz_questions qq ON qq.quiz_id = q.quiz_id
                LEFT JOIN question_bank_questions b ON b.bank_question_id = qq.bank_question_id
                WHERE q.created_by = ? AND q.status IN ('PUBLISHED', 'CLOSED')
                  AND (q.available_from IS NULL OR q.available_from <= CURRENT_TIMESTAMP)
                GROUP BY q.quiz_id, q.subject_id, q.title, q.available_from, q.created_at, q.available_until, q.created_by
                ORDER BY q.quiz_id
                """, teacherId)) {
            ownedAssessments.add("Q-" + text(row, "quiz_id"));
            addAssessment(row, "Q-", "quiz_id", "QUIZ", subjectIds, assessments, deadlines, notices);
        }

        var results = new LinkedHashMap<String, Result>();
        for (var row : jdbc.queryForList("""
                SELECT s.student_user_id, s.assignment_id, s.status, s.score, s.feedback, u.full_name
                FROM assignment_submissions s JOIN assignments a ON a.assignment_id = s.assignment_id
                JOIN users u ON u.user_id = s.student_user_id
                """ + TeacherAnalyticsVisibility.REVIEWER_JOIN + " WHERE "
                + TeacherAnalyticsVisibility.ASSIGNMENT_REVIEW + """
                AND u.role = 'STUDENT'
                AND (s.status IN ('SUBMITTED', 'GRADED')
                     OR (a.created_by = analytics_reviewer.user_id AND s.status = 'RETURNED'))
                """, teacherId)) {
            String student = text(row, "student_user_id");
            String assessment = "A-" + text(row, "assignment_id");
            addParticipant(row, student, assessment, studentsById, assessments);
            if (applicable(student, assessment, studentsById, assessments)) {
                results.put(key(student, assessment), result(row, student, assessment, assessments, notices));
            }
        }
        var chosenAttempts = new HashMap<String, Long>();
        var gradedAttempts = new HashSet<Long>();
        for (var row : jdbc.queryForList("""
                SELECT t.attempt_id, t.student_user_id, t.quiz_id, t.status, t.score, u.full_name
                FROM quiz_attempts t JOIN quizzes q ON q.quiz_id = t.quiz_id
                JOIN users u ON u.user_id = t.student_user_id
                WHERE q.created_by = ? AND u.role = 'STUDENT'
                  AND t.status IN ('SUBMITTED', 'GRADED')
                ORDER BY t.attempt_number DESC, t.attempt_id DESC
                """, teacherId)) {
            String student = text(row, "student_user_id");
            String assessment = "Q-" + text(row, "quiz_id");
            addParticipant(row, student, assessment, studentsById, assessments);
            String key = key(student, assessment);
            if (!applicable(student, assessment, studentsById, assessments) || chosenAttempts.containsKey(key)) { continue; }
            long attempt = ((Number) row.get("attempt_id")).longValue();
            chosenAttempts.put(key, attempt);
            Result result = result(row, student, assessment, assessments, notices);
            results.put(key, result);
            if ("GRADED".equals(result.status())) { gradedAttempts.add(attempt); }
        }

        var questions = new LinkedHashMap<String, Question>();
        for (var row : jdbc.queryForList("""
                SELECT qq.question_id, qq.quiz_id, qq.position, b.difficulty
                FROM quiz_questions qq JOIN quizzes q ON q.quiz_id = qq.quiz_id
                JOIN question_bank_questions b ON b.bank_question_id = qq.bank_question_id
                WHERE q.created_by = ? ORDER BY qq.quiz_id, qq.position, qq.question_id
                """, teacherId)) {
            String assessment = "Q-" + text(row, "quiz_id");
            if (assessments.containsKey(assessment)) {
                questions.put(text(row, "question_id"), new Question(assessment,
                        "Question " + text(row, "position"), text(row, "difficulty"), 0, 0));
            }
        }
        for (var row : jdbc.queryForList("""
                SELECT t.attempt_id, t.student_user_id, t.quiz_id, a.question_id, a.awarded_points, b.points
                FROM quiz_answers a JOIN quiz_attempts t ON t.attempt_id = a.attempt_id
                JOIN quizzes q ON q.quiz_id = t.quiz_id
                JOIN users u ON u.user_id = t.student_user_id
                JOIN quiz_questions qq ON qq.question_id = a.question_id AND qq.quiz_id = t.quiz_id
                JOIN question_bank_questions b ON b.bank_question_id = qq.bank_question_id
                WHERE q.created_by = ? AND u.role = 'STUDENT'
                  AND t.status = 'GRADED' AND a.awarded_points IS NOT NULL
                """, teacherId)) {
            long attempt = ((Number) row.get("attempt_id")).longValue();
            String assessment = "Q-" + text(row, "quiz_id");
            Long chosen = chosenAttempts.get(key(text(row, "student_user_id"), assessment));
            String questionId = text(row, "question_id");
            Question question = questions.get(questionId);
            double awarded = ((Number) row.get("awarded_points")).doubleValue();
            double maximum = ((Number) row.get("points")).doubleValue();
            if (question == null || chosen == null || chosen != attempt || !gradedAttempts.contains(attempt)
                    || maximum <= 0 || !Double.isFinite(awarded) || awarded < 0 || awarded > maximum) { continue; }
            questions.put(questionId, new Question(question.assessmentId(), question.label(), question.difficulty(),
                    question.answered() + 1, question.correct() + (Double.compare(awarded, maximum) == 0 ? 1 : 0)));
        }

        var eligibleAssessments = assessments.values().stream().map(assessment -> {
            Set<String> eligible = new HashSet<>();
            LocalDateTime deadline = deadlines.get(assessment.id());
            for (Student student : studentsById.values()) {
                if (!student.subjectIds().contains(assessment.subjectId())) { continue; }
                LocalDateTime joined = enrollmentTimes.get(key(student.id(), assessment.subjectId()));
                if ((ownedAssessments.contains(assessment.id()) && joined != null && (deadline == null || !joined.isAfter(deadline)))
                        || results.containsKey(key(student.id(), assessment.id()))) {
                    eligible.add(student.id());
                }
            }
            boolean rosterKnown = ownedAssessments.contains(assessment.id()) && studentsById.keySet().stream()
                    .anyMatch(id -> enrollmentTimes.containsKey(key(id, assessment.subjectId()))) && eligible.stream()
                    .allMatch(id -> enrollmentTimes.containsKey(key(id, assessment.subjectId())));
            return new Assessment(assessment.id(), assessment.title(), "", assessment.subjectId(), assessment.type(),
                    assessment.date(), assessment.maximumScore(), eligible, rosterKnown);
        }).toList();
        return new Snapshot(List.of(), subjects, new ArrayList<>(studentsById.values()), eligibleAssessments,
                new ArrayList<>(results.values()), new ArrayList<>(questions.values()), false, notices);
    }

    private void addParticipant(Map<String, Object> row, String student, String assessment,
            Map<String, Student> students, Map<String, Assessment> assessments) {
        Assessment item = assessments.get(assessment);
        if (item == null) { return; }
        Student previous = students.get(student);
        Set<String> subjects = new HashSet<>(previous == null ? Set.of() : previous.subjectIds());
        subjects.add(item.subjectId());
        students.put(student, new Student(student, "User " + student, text(row, "full_name"), "", subjects));
    }

    private void addAssessment(Map<String, Object> row, String prefix, String idColumn, String type,
            Set<String> subjects, Map<String, Assessment> assessments, Map<String, LocalDateTime> deadlines, List<String> notices) {
        if (!subjects.contains(text(row, "subject_id"))) { return; }
        double maximum = ((Number) row.get("max_score")).doubleValue();
        if (!Double.isFinite(maximum) || maximum <= 0) {
            addNotice(notices, "Assessments without a positive maximum score are excluded from normalized analytics.");
            return;
        }
        String id = prefix + text(row, idColumn);
        assessments.put(id, new Assessment(id, text(row, "title"), "", text(row, "subject_id"), type,
                date(row.get("assessment_date")), maximum));
        deadlines.put(id, time(row.get("deadline")));
    }

    private Result result(Map<String, Object> row, String student, String assessment,
            Map<String, Assessment> assessments, List<String> notices) {
        Double score = row.get("score") instanceof Number number ? number.doubleValue() : null;
        boolean graded = "GRADED".equals(text(row, "status"));
        if (graded && (score == null || !Double.isFinite(score) || score < 0 || score > assessments.get(assessment).maximumScore())) {
            addNotice(notices, "Some graded work has missing or invalid scores and is excluded from score calculations.");
            graded = false;
        }
        return new Result(student, assessment, graded ? "GRADED" : "SUBMITTED", graded ? score : null,
                graded && row.get("feedback") != null ? text(row, "feedback") : "");
    }

    private static boolean applicable(String student, String assessment, Map<String, Student> students,
            Map<String, Assessment> assessments) {
        return students.containsKey(student) && assessments.containsKey(assessment)
                && students.get(student).subjectIds().contains(assessments.get(assessment).subjectId());
    }
    private static String key(String first, String second) { return first + ":" + second; }
    private static String text(Map<String, Object> row, String name) { return String.valueOf(row.get(name)); }
    private static LocalDate date(Object value) { return value == null ? null : LocalDate.parse(value.toString().substring(0, 10)); }
    private static LocalDateTime time(Object value) {
        if (value == null) { return null; }
        if (value instanceof java.sql.Timestamp timestamp) { return timestamp.toLocalDateTime(); }
        if (value instanceof LocalDateTime timestamp) { return timestamp; }
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }
    private static void addNotice(List<String> notices, String notice) { if (!notices.contains(notice)) { notices.add(notice); } }
}
