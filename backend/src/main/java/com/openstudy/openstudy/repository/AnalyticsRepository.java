package com.openstudy.openstudy.repository;

import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.*;

import com.openstudy.openstudy.dto.analytics.AnalyticsFilter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC queries for the analytics module. READ-ONLY: every statement is a SELECT over existing tables
 * (quizzes, quiz_questions, quiz_options, quiz_attempts, quiz_answers, assignments,
 * assignment_submissions, subjects, subject_teachers, subject_enrollments). The module creates no tables,
 * views, indexes or other database objects.
 *
 * <p>The counting rules (latest completed attempt, finalized assignments only, percentage per result)
 * live in {@link #RESULTS}, a derived table every query selects from.
 *
 * <p>Access control lives in the WHERE clause: student queries filter on the student's own id;
 * teacher queries only return rows whose subject the teacher is assigned to (subject_teachers) or
 * whose assessment the teacher created. All values are bound parameters; no request text is ever
 * concatenated into SQL.
 */
@Repository
@Profile("!mock")
public class AnalyticsRepository {

    /**
     * One row per counted result, as a derived table.
     * Quiz: latest attempt (highest attempt_number) per quiz and student, status SUBMITTED/GRADED, score present;
     *       max score = SUM(quiz_questions.points).
     * Assignment: status GRADED/RETURNED (finalized), score present, assignments.max_score > 0.
     * percentage = score / max_score * 100, clamped to 0..100.
     */
    static final String RESULTS =
            "(SELECT 'QUIZ' AS assessment_type, q.quiz_id AS assessment_id, a.attempt_id AS result_id, q.title AS title, "
            + "q.subject_id AS subject_id, s.subject_name AS subject_name, q.created_by AS created_by, "
            + "a.student_user_id AS student_user_id, a.score AS score, m.max_points AS max_score, "
            + "ROUND(LEAST(100, GREATEST(0, a.score / m.max_points * 100)), 2) AS percentage, "
            + "COALESCE(a.submitted_at, a.started_at) AS completed_at "
            + "FROM (SELECT qa.*, ROW_NUMBER() OVER (PARTITION BY qa.quiz_id, qa.student_user_id "
            + "ORDER BY qa.attempt_number DESC) AS rn FROM quiz_attempts qa "
            + "WHERE qa.status IN ('SUBMITTED', 'GRADED') AND qa.score IS NOT NULL) a "
            + "JOIN quizzes q ON q.quiz_id = a.quiz_id "
            + "JOIN (SELECT quiz_id, SUM(points) AS max_points FROM quiz_questions GROUP BY quiz_id) m "
            + "  ON m.quiz_id = q.quiz_id AND m.max_points > 0 "
            + "JOIN subjects s ON s.subject_id = q.subject_id "
            + "WHERE a.rn = 1 "
            + "UNION ALL "
            + "SELECT 'ASSIGNMENT', asg.assignment_id, sub.submission_id, asg.title, asg.subject_id, s.subject_name, "
            + "asg.created_by, sub.student_user_id, sub.score, asg.max_score, "
            + "ROUND(LEAST(100, GREATEST(0, sub.score / asg.max_score * 100)), 2), "
            + "COALESCE(sub.submitted_at, sub.graded_at, sub.created_at) "
            + "FROM assignment_submissions sub "
            + "JOIN assignments asg ON asg.assignment_id = sub.assignment_id AND asg.max_score > 0 "
            + "JOIN subjects s ON s.subject_id = asg.subject_id "
            + "WHERE sub.status IN ('GRADED', 'RETURNED') AND sub.score IS NOT NULL)";

    /** Quiz-only slice of RESULTS joined to questions: accuracy per question (same counted attempts). */
    static final String QUESTION_RESULTS =
            "(SELECT r.assessment_id AS quiz_id, r.title AS quiz_title, r.subject_id AS subject_id, "
            + "r.subject_name AS subject_name, r.created_by AS created_by, qq.question_id AS question_id, "
            + "qq.position AS position, qq.question_text AS question_text, COUNT(*) AS attempts, "
            + "SUM(CASE WHEN COALESCE(ans.awarded_points, CASE WHEN o.is_correct THEN qq.points ELSE 0 END) >= qq.points "
            + "THEN 1 ELSE 0 END) AS correct_count "
            + "FROM " + RESULTS + " r "
            + "JOIN quiz_questions qq ON qq.quiz_id = r.assessment_id AND qq.points > 0 "
            + "LEFT JOIN quiz_answers ans ON ans.attempt_id = r.result_id AND ans.question_id = qq.question_id "
            + "LEFT JOIN quiz_options o ON o.option_id = ans.selected_option_id "
            + "WHERE r.assessment_type = 'QUIZ' "
            + "GROUP BY r.assessment_id, r.title, r.subject_id, r.subject_name, r.created_by, "
            + "qq.question_id, qq.position, qq.question_text)";

    /** Subjects a teacher may see: assigned via subject_teachers or owning an assessment in them. */
    private static final String AUTHORIZED_SUBJECTS =
            "SELECT st.subject_id FROM subject_teachers st WHERE st.teacher_id = ? "
            + "UNION SELECT q.subject_id FROM quizzes q WHERE q.created_by = ? "
            + "UNION SELECT a.subject_id FROM assignments a WHERE a.created_by = ?";

    private static final String TEACHER_SCOPE =
            "(v.created_by = ? OR v.subject_id IN (SELECT st.subject_id FROM subject_teachers st WHERE st.teacher_id = ?))";

    private final JdbcTemplate jdbc;

    public AnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ WHERE builder

    private static final class Where {
        final StringBuilder sql = new StringBuilder();
        final List<Object> args = new ArrayList<>();

        Where(String base, Object... baseArgs) {
            sql.append(base);
            args.addAll(List.of(baseArgs));
        }

        Where and(String fragment, Object arg) {
            sql.append(" AND ").append(fragment);
            args.add(arg);
            return this;
        }

        Object[] argArray() {
            return args.toArray();
        }
    }

    private static Where applyFilter(Where w, AnalyticsFilter f, String alias) {
        if (f == null) {
            return w;
        }
        if (f.subjectId() != null) {
            w.and(alias + ".subject_id = ?", f.subjectId());
        }
        if (f.type() != null) {
            w.and(alias + ".assessment_type = ?", f.type());
        }
        if (f.from() != null) {
            w.and(alias + ".completed_at >= ?", Timestamp.valueOf(f.from().atStartOfDay()));
        }
        if (f.to() != null) {
            w.and(alias + ".completed_at < ?", Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        }
        return w;
    }

    private static Where student(long studentId, AnalyticsFilter f) {
        return applyFilter(new Where("v.student_user_id = ?", studentId), f, "v");
    }

    private static Where teacher(long teacherId, AnalyticsFilter f) {
        return applyFilter(new Where(TEACHER_SCOPE, teacherId, teacherId), f, "v");
    }

    // ------------------------------------------------------------------ shared row mappers

    private static Double dbl(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static SummaryStats mapSummary(ResultSet rs) throws SQLException {
        return new SummaryStats(rs.getLong("n"), dbl(rs, "avg_p"), dbl(rs, "min_p"), dbl(rs, "max_p"),
                rs.getLong("quiz_n"), dbl(rs, "quiz_avg"), rs.getLong("asg_n"), dbl(rs, "asg_avg"),
                rs.getLong("students"));
    }

    private static final String SUMMARY_SELECT =
            "SELECT COUNT(*) AS n, AVG(v.percentage) AS avg_p, MIN(v.percentage) AS min_p, MAX(v.percentage) AS max_p, "
            + "COALESCE(SUM(v.assessment_type = 'QUIZ'), 0) AS quiz_n, "
            + "AVG(CASE WHEN v.assessment_type = 'QUIZ' THEN v.percentage END) AS quiz_avg, "
            + "COALESCE(SUM(v.assessment_type = 'ASSIGNMENT'), 0) AS asg_n, "
            + "AVG(CASE WHEN v.assessment_type = 'ASSIGNMENT' THEN v.percentage END) AS asg_avg, "
            + "COUNT(DISTINCT v.student_user_id) AS students FROM " + RESULTS + " v WHERE ";

    private static final String SUBJECT_SELECT =
            "SELECT v.subject_id, v.subject_name, COUNT(*) AS n, AVG(v.percentage) AS avg_p, MIN(v.percentage) AS min_p, "
            + "MAX(v.percentage) AS max_p, COALESCE(SUM(v.assessment_type = 'QUIZ'), 0) AS quiz_n, "
            + "AVG(CASE WHEN v.assessment_type = 'QUIZ' THEN v.percentage END) AS quiz_avg, "
            + "COALESCE(SUM(v.assessment_type = 'ASSIGNMENT'), 0) AS asg_n, "
            + "AVG(CASE WHEN v.assessment_type = 'ASSIGNMENT' THEN v.percentage END) AS asg_avg "
            + "FROM " + RESULTS + " v WHERE ";

    private static final String SUBJECT_TAIL = " GROUP BY v.subject_id, v.subject_name ORDER BY v.subject_name";

    private static SubjectStat mapSubject(ResultSet rs, int i) throws SQLException {
        return new SubjectStat(rs.getLong("subject_id"), rs.getString("subject_name"), rs.getLong("n"),
                dbl(rs, "avg_p"), dbl(rs, "min_p"), dbl(rs, "max_p"), rs.getLong("quiz_n"), dbl(rs, "quiz_avg"),
                rs.getLong("asg_n"), dbl(rs, "asg_avg"));
    }

    private static final String MONTH_SELECT =
            "SELECT DATE_FORMAT(v.completed_at, '%Y-%m') AS month, COUNT(*) AS n, AVG(v.percentage) AS avg_p, "
            + "AVG(CASE WHEN v.assessment_type = 'QUIZ' THEN v.percentage END) AS quiz_avg, "
            + "AVG(CASE WHEN v.assessment_type = 'ASSIGNMENT' THEN v.percentage END) AS asg_avg "
            + "FROM " + RESULTS + " v WHERE ";
    private static final String MONTH_TAIL = " GROUP BY month ORDER BY month";

    private static MonthStat mapMonth(ResultSet rs, int i) throws SQLException {
        return new MonthStat(rs.getString("month"), rs.getLong("n"), dbl(rs, "avg_p"), dbl(rs, "quiz_avg"), dbl(rs, "asg_avg"));
    }

    private static final String ROW_SELECT =
            "SELECT v.assessment_type, v.assessment_id, v.title, v.subject_name, v.student_user_id, v.score, "
            + "v.max_score, v.percentage, v.completed_at FROM " + RESULTS + " v WHERE ";
    private static final String ROW_TAIL = " ORDER BY v.completed_at, v.result_id LIMIT ?";

    private static ResultRow mapRow(ResultSet rs, int i) throws SQLException {
        Timestamp ts = rs.getTimestamp("completed_at");
        return new ResultRow(rs.getString("assessment_type"), rs.getLong("assessment_id"), rs.getString("title"),
                rs.getString("subject_name"), rs.getLong("student_user_id"), rs.getDouble("score"),
                rs.getDouble("max_score"), rs.getDouble("percentage"), ts == null ? null : ts.toLocalDateTime());
    }

    private static final String BIN_SELECT =
            "SELECT LEAST(FLOOR(v.percentage / 10), 9) AS bin, COUNT(*) AS n FROM " + RESULTS + " v WHERE ";
    private static final String BIN_TAIL = " GROUP BY bin";

    private static final String BAND_SELECT =
            "SELECT COALESCE(SUM(v.percentage > 90), 0) AS a, "
            + "COALESCE(SUM(v.percentage > 70 AND v.percentage <= 90), 0) AS b, "
            + "COALESCE(SUM(v.percentage > 50 AND v.percentage <= 70), 0) AS c, "
            + "COALESCE(SUM(v.percentage <= 50), 0) AS d FROM " + RESULTS + " v WHERE ";

    // ------------------------------------------------------------------ student queries

    public SummaryStats studentSummary(long studentId, AnalyticsFilter f) {
        Where w = student(studentId, f);
        return jdbc.queryForObject(SUMMARY_SELECT + w.sql, (rs, i) -> mapSummary(rs), w.argArray());
    }

    public List<SubjectStat> studentSubjects(long studentId, AnalyticsFilter f) {
        Where w = student(studentId, f);
        return jdbc.query(SUBJECT_SELECT + w.sql + SUBJECT_TAIL, AnalyticsRepository::mapSubject, w.argArray());
    }

    public List<MonthStat> studentMonthly(long studentId, AnalyticsFilter f) {
        Where w = student(studentId, f);
        return jdbc.query(MONTH_SELECT + w.sql + MONTH_TAIL, AnalyticsRepository::mapMonth, w.argArray());
    }

    public List<ResultRow> studentResults(long studentId, AnalyticsFilter f, int limit) {
        Where w = student(studentId, f);
        return jdbc.query(ROW_SELECT + w.sql + ROW_TAIL, AnalyticsRepository::mapRow, withLimit(w, limit));
    }

    public List<BandStat> studentBands(long studentId, AnalyticsFilter f) {
        Where w = student(studentId, f);
        long[] c = jdbc.queryForObject(BAND_SELECT + w.sql,
                (rs, i) -> new long[] {rs.getLong("a"), rs.getLong("b"), rs.getLong("c"), rs.getLong("d")}, w.argArray());
        long total = c[0] + c[1] + c[2] + c[3];
        String[] labels = {"Above 90%", "70% - 90%", "50% - 70%", "Below 50%"};
        List<BandStat> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            out.add(new BandStat(labels[i], c[i], total == 0 ? 0 : Math.round(c[i] * 10000.0 / total) / 100.0));
        }
        return out;
    }

    /**
     * Rank by average percentage among all students that have at least one completed result.
     * Returns only this student's rank and the class size, never other students' scores.
     */
    public Optional<RankInfo> studentRank(long studentId) {
        String sql = "WITH per AS (SELECT student_user_id, AVG(percentage) AS avg_p FROM " + RESULTS
                + " r GROUP BY student_user_id) "
                + "SELECT (SELECT COUNT(*) FROM per p2 WHERE p2.avg_p > me.avg_p) + 1 AS rnk, "
                + "(SELECT COUNT(*) FROM per) AS class_size FROM per me WHERE me.student_user_id = ?";
        List<RankInfo> r = jdbc.query(sql, (rs, i) -> {
            int rank = rs.getInt("rnk");
            int size = rs.getInt("class_size");
            double pct = size <= 1 ? 100.0 : Math.round((size - rank) * 10000.0 / (size - 1)) / 100.0;
            return new RankInfo(rank, size, pct);
        }, studentId);
        return r.stream().findFirst();
    }

    // ------------------------------------------------------------------ teacher queries

    public boolean teacherCanAccessSubject(long teacherId, long subjectId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM (" + AUTHORIZED_SUBJECTS + ") a WHERE a.subject_id = ?",
                Integer.class, teacherId, teacherId, teacherId, subjectId);
        return n != null && n > 0;
    }

    public List<SubjectOption> teacherSubjects(long teacherId) {
        return jdbc.query("SELECT s.subject_id, s.subject_name FROM subjects s WHERE s.subject_id IN ("
                        + AUTHORIZED_SUBJECTS + ") ORDER BY s.subject_name",
                (rs, i) -> new SubjectOption(rs.getLong("subject_id"), rs.getString("subject_name")),
                teacherId, teacherId, teacherId);
    }

    public SummaryStats teacherSummary(long teacherId, AnalyticsFilter f) {
        Where w = teacher(teacherId, f);
        return jdbc.queryForObject(SUMMARY_SELECT + w.sql, (rs, i) -> mapSummary(rs), w.argArray());
    }

    public List<SubjectStat> teacherSubjectStats(long teacherId, AnalyticsFilter f) {
        Where w = teacher(teacherId, f);
        return jdbc.query(SUBJECT_SELECT + w.sql + SUBJECT_TAIL, AnalyticsRepository::mapSubject, w.argArray());
    }

    public List<MonthStat> teacherMonthly(long teacherId, AnalyticsFilter f) {
        Where w = teacher(teacherId, f);
        return jdbc.query(MONTH_SELECT + w.sql + MONTH_TAIL, AnalyticsRepository::mapMonth, w.argArray());
    }

    public List<ResultRow> teacherResults(long teacherId, AnalyticsFilter f, int limit) {
        Where w = teacher(teacherId, f);
        return jdbc.query(ROW_SELECT + w.sql + ROW_TAIL, AnalyticsRepository::mapRow, withLimit(w, limit));
    }

    public Distribution teacherDistribution(long teacherId, AnalyticsFilter f) {
        Where w = teacher(teacherId, f);
        long[] counts = new long[10];
        jdbc.query(BIN_SELECT + w.sql + BIN_TAIL, rs -> {
            int bin = rs.getInt("bin");
            if (bin >= 0 && bin < 10) {
                counts[bin] = rs.getLong("n");
            }
        }, w.argArray());
        long total = 0;
        for (long c : counts) {
            total += c;
        }
        List<BinStat> bins = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int from = i * 10;
            int to = i == 9 ? 100 : from + 10;
            String label = i == 9 ? "90-100" : from + "-" + (to - 1);
            bins.add(new BinStat(label, from, to, counts[i], total == 0 ? 0 : Math.round(counts[i] * 10000.0 / total) / 100.0));
        }
        return new Distribution(bins, total);
    }

    /** Students enrolled (ENROLLED status) in the teacher's subjects, optionally narrowed to one subject. */
    public long teacherEnrolledStudents(long teacherId, AnalyticsFilter f) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT se.student_user_id) FROM subject_enrollments se "
                + "WHERE se.enrollment_status = 'ENROLLED' AND se.subject_id IN (" + AUTHORIZED_SUBJECTS + ")");
        List<Object> args = new ArrayList<>(List.of(teacherId, teacherId, teacherId));
        if (f != null && f.subjectId() != null) {
            sql.append(" AND se.subject_id = ?");
            args.add(f.subjectId());
        }
        Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    /** Per published assessment: enrolled students versus students with a counted result. */
    public List<AssessmentParticipation> teacherAssessmentParticipation(long teacherId, AnalyticsFilter f) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.assessment_type, t.assessment_id, t.title, s.subject_name, "
                + "(SELECT COUNT(*) FROM subject_enrollments se WHERE se.subject_id = t.subject_id "
                + "   AND se.enrollment_status = 'ENROLLED') AS enrolled, "
                + "COUNT(v.student_user_id) AS completed, AVG(v.percentage) AS avg_p "
                + "FROM (SELECT 'QUIZ' AS assessment_type, quiz_id AS assessment_id, title, subject_id, created_by "
                + "      FROM quizzes WHERE status IN ('PUBLISHED', 'CLOSED') "
                + "      UNION ALL SELECT 'ASSIGNMENT', assignment_id, title, subject_id, created_by "
                + "      FROM assignments WHERE status IN ('PUBLISHED', 'CLOSED')) t "
                + "JOIN subjects s ON s.subject_id = t.subject_id "
                + "LEFT JOIN " + RESULTS + " v ON v.assessment_type = t.assessment_type AND v.assessment_id = t.assessment_id");
        List<Object> args = new ArrayList<>();
        if (f != null && f.from() != null) {
            sql.append(" AND v.completed_at >= ?");
            args.add(Timestamp.valueOf(f.from().atStartOfDay()));
        }
        if (f != null && f.to() != null) {
            sql.append(" AND v.completed_at < ?");
            args.add(Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        }
        sql.append(" WHERE (t.created_by = ? OR t.subject_id IN "
                + "(SELECT st.subject_id FROM subject_teachers st WHERE st.teacher_id = ?))");
        args.add(teacherId);
        args.add(teacherId);
        if (f != null && f.subjectId() != null) {
            sql.append(" AND t.subject_id = ?");
            args.add(f.subjectId());
        }
        if (f != null && f.type() != null) {
            sql.append(" AND t.assessment_type = ?");
            args.add(f.type());
        }
        sql.append(" GROUP BY t.assessment_type, t.assessment_id, t.title, s.subject_name, t.subject_id "
                + "ORDER BY s.subject_name, t.assessment_type, t.title");
        return jdbc.query(sql.toString(), (rs, i) -> {
            long enrolled = rs.getLong("enrolled");
            long completed = rs.getLong("completed");
            Double rate = enrolled == 0 ? null : Math.min(100.0, Math.round(completed * 10000.0 / enrolled) / 100.0);
            return new AssessmentParticipation(rs.getString("assessment_type"), rs.getLong("assessment_id"),
                    rs.getString("title"), rs.getString("subject_name"), enrolled, completed, dbl(rs, "avg_p"), rate);
        }, args.toArray());
    }

    /** Question-level accuracy for quizzes the teacher may see (lowest correct-rate first). */
    public List<QuestionStat> teacherQuestionStats(long teacherId, AnalyticsFilter f, int limit) {
        StringBuilder sql = new StringBuilder("SELECT q.quiz_id, q.quiz_title, q.subject_name, q.position, q.question_text, "
                + "q.attempts, q.correct_count FROM " + QUESTION_RESULTS + " q "
                + "WHERE (q.created_by = ? OR q.subject_id IN (SELECT st.subject_id FROM subject_teachers st WHERE st.teacher_id = ?))");
        List<Object> args = new ArrayList<>(List.of(teacherId, teacherId));
        if (f != null && f.subjectId() != null) {
            sql.append(" AND q.subject_id = ?");
            args.add(f.subjectId());
        }
        sql.append(" ORDER BY (q.correct_count / q.attempts) ASC, q.attempts DESC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), (rs, i) -> {
            long attempts = rs.getLong("attempts");
            long correct = rs.getLong("correct_count");
            double rate = attempts == 0 ? 0 : Math.round(correct * 10000.0 / attempts) / 100.0;
            boolean reliable = attempts >= 5;
            String text = rs.getString("question_text");
            if (text != null && text.length() > 160) {
                text = text.substring(0, 157) + "...";
            }
            String difficulty = !reliable ? "Too few attempts" : rate < 40 ? "Hard" : rate < 70 ? "Medium" : "Easy";
            return new QuestionStat(rs.getLong("quiz_id"), rs.getString("quiz_title"), rs.getString("subject_name"),
                    rs.getInt("position"), text, attempts, correct, rate, difficulty, reliable);
        }, args.toArray());
    }

    private static Object[] withLimit(Where w, int limit) {
        List<Object> a = new ArrayList<>(w.args);
        a.add(limit);
        return a.toArray();
    }
}
