package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Analytics-only adapter to the existing login session; it never changes authentication. */
@Component
@Profile("!mock")
@ConditionalOnProperty(name = "openstudy.teacher-analytics.provider", havingValue = "jdbc")
public class JdbcTeacherAnalyticsAccessProvider implements TeacherAnalyticsAccessProvider {
    private final JdbcTemplate jdbc;

    public JdbcTeacherAnalyticsAccessProvider(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public TeacherAnalyticsScope resolve(Principal principal) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in with a teacher account.");
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAnalyticsScope resolveRequest(HttpServletRequest request) {
        var session = request.getSession(false);
        Object identity = session == null ? null : session.getAttribute("loggedInUser");
        if (!(identity instanceof User user) || user.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in with a teacher account.");
        }
        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var teachers = jdbc.query("""
                SELECT user_id, full_name, email FROM users
                WHERE user_id = ? AND role = 'TEACHER' AND account_status = 'ACTIVE'
                """, (row, index) -> new TeacherIdentityDTO(row.getString("user_id"),
                        row.getString("full_name"), row.getString("email"), "Teacher", false), user.getUserId());
        if (teachers.size() != 1) { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
        Set<String> subjects = new HashSet<>(jdbc.queryForList("""
                SELECT st.subject_id FROM subject_teachers st
                JOIN subjects s ON s.subject_id = st.subject_id
                WHERE st.teacher_id = ?
                """, String.class, user.getUserId()));
        // The verified Aiven schema has subject assignments, but no class/cohort relationship.
        return new TeacherAnalyticsScope(teachers.getFirst(), Set.of(), subjects);
    }
}
