package com.openstudy.openstudy.provider;

import com.openstudy.openstudy.dto.DashboardStatsDTO;
import com.openstudy.openstudy.dto.RecentActivityDTO;
import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.UpcomingDeadlineDTO;
import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Teacher dashboard figures read from the database for the signed-in teacher only.
 * A new teacher with no content sees zeros and empty lists.
 */
@Component
@ConditionalOnProperty(name = "openstudy.dashboard.provider", havingValue = "jdbc")
public class JdbcTeacherDashboardDataProvider implements TeacherDashboardDataProvider {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JdbcTemplate db;

    public JdbcTeacherDashboardDataProvider(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public TeacherIdentityDTO getTeacherIdentity() {
        User user = sessionUser();
        if (user == null) {
            return new TeacherIdentityDTO("teacher", "Teacher", "", "Teacher", false);
        }
        String name = user.getFullName() == null || user.getFullName().isBlank() ? "Teacher" : user.getFullName();
        return new TeacherIdentityDTO(
                user.getUserId() == null ? "teacher" : String.valueOf(user.getUserId()),
                name,
                user.getEmail() == null ? "" : user.getEmail(),
                "Teacher",
                false);
    }

    @Override
    public DashboardStatsDTO getStatistics() {
        long teacherId = teacherId();
        return new DashboardStatsDTO(
                count("SELECT COUNT(*) FROM quizzes WHERE created_by=?", teacherId),
                count("SELECT COUNT(*) FROM assignments WHERE created_by=?", teacherId),
                count("SELECT COUNT(DISTINCT e.student_user_id) FROM subject_enrollments e "
                        + "WHERE e.enrollment_status='ENROLLED' AND e.subject_id IN ("
                        + "SELECT subject_id FROM subject_teachers WHERE teacher_id=? "
                        + "UNION SELECT subject_id FROM assignments WHERE created_by=? "
                        + "UNION SELECT subject_id FROM quizzes WHERE created_by=?)", teacherId, teacherId, teacherId),
                count("SELECT COUNT(*) FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id "
                        + "WHERE a.created_by=? AND s.status='SUBMITTED'", teacherId));
    }

    @Override
    public List<RecentActivityDTO> getRecentActivity() {
        long teacherId = teacherId();
        return db.query(
                "SELECT * FROM ("
                        + "SELECT CONCAT('quiz-', q.quiz_id) id, 'QUIZ_PUBLISHED' type, 'Quiz published' title, "
                        + "CONCAT('\"', q.title, '\" is available to students.') description, q.updated_at at "
                        + "FROM quizzes q WHERE q.created_by=? AND q.status='PUBLISHED' "
                        + "UNION ALL "
                        + "SELECT CONCAT('asg-', a.assignment_id), 'ASSIGNMENT_CREATED', 'Assignment created', "
                        + "CONCAT('\"', a.title, '\" was created.'), a.created_at "
                        + "FROM assignments a WHERE a.created_by=? "
                        + "UNION ALL "
                        + "SELECT CONCAT('sub-', s.submission_id), 'ASSIGNMENT_SUBMITTED', 'Assignment submitted', "
                        + "CONCAT(u.full_name, ' submitted \"', a.title, '\".'), s.submitted_at "
                        + "FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id "
                        + "JOIN users u ON u.user_id=s.student_user_id "
                        + "WHERE a.created_by=? AND s.status IN ('SUBMITTED','GRADED') AND s.submitted_at IS NOT NULL"
                        + ") activity ORDER BY at DESC LIMIT 8",
                (rs, row) -> {
                    LocalDateTime at = toLocal(rs.getTimestamp("at"));
                    return new RecentActivityDTO(rs.getString("id"), rs.getString("type"), rs.getString("title"),
                            rs.getString("description"), at, at == null ? "" : at.format(DISPLAY));
                },
                teacherId, teacherId, teacherId);
    }

    @Override
    public List<UpcomingDeadlineDTO> getUpcomingDeadlines() {
        long teacherId = teacherId();
        LocalDateTime now = LocalDateTime.now();
        return db.query(
                "SELECT * FROM ("
                        + "SELECT CONCAT('asg-', a.assignment_id) id, 'ASSIGNMENT' type, a.title, a.due_at due, s.subject_name course "
                        + "FROM assignments a JOIN subjects s ON s.subject_id=a.subject_id "
                        + "WHERE a.created_by=? AND a.status='PUBLISHED' AND a.due_at>=? "
                        + "UNION ALL "
                        + "SELECT CONCAT('quiz-', q.quiz_id), 'QUIZ', q.title, q.available_until, s.subject_name "
                        + "FROM quizzes q JOIN subjects s ON s.subject_id=q.subject_id "
                        + "WHERE q.created_by=? AND q.status='PUBLISHED' AND q.available_until>=?"
                        + ") deadlines ORDER BY due ASC LIMIT 5",
                (rs, row) -> {
                    LocalDateTime due = toLocal(rs.getTimestamp("due"));
                    return new UpcomingDeadlineDTO(rs.getString("id"), rs.getString("type"), rs.getString("title"),
                            due, due == null ? "" : due.format(DISPLAY), rs.getString("course"));
                },
                teacherId, now, teacherId, now);
    }

    private int count(String sql, Object... args) {
        Integer value = db.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private long teacherId() {
        User user = sessionUser();
        return user == null || user.getUserId() == null ? -1L : user.getUserId();
    }

    private User sessionUser() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) return null;
        HttpSession session = servlet.getRequest().getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
