
package com.openstudy.openstudy.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.openstudy.openstudy.model.StudySession;

@Repository
public class StudySessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudySessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Convert a database row into a StudySession object
    private final RowMapper<StudySession> sessionRowMapper =
            (rs, rowNum) -> {
                StudySession session = new StudySession();

                session.setSessionId(rs.getLong("session_id"));
                session.setStudentId(rs.getLong("student_id"));

                long subjectId = rs.getLong("subject_id");
                session.setSubjectId(rs.wasNull() ? null : subjectId);

                long assignmentId = rs.getLong("assignment_id");
                session.setAssignmentId(
                        rs.wasNull() ? null : assignmentId
                );

                session.setStartTime(
                        rs.getTimestamp("start_time").toLocalDateTime()
                );
                session.setEndTime(
                        rs.getTimestamp("end_time").toLocalDateTime()
                );
                session.setNotes(rs.getString("notes"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    session.setCreatedAt(createdAt.toLocalDateTime());
                }

                return session;
            };

    // Create a study session
    public StudySession save(StudySession session) {
        String sql = """
                INSERT INTO study_sessions
                (student_id, subject_id, assignment_id,
                 start_time, end_time, notes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, session.getStudentId());

            if (session.getSubjectId() == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, session.getSubjectId());
            }

            if (session.getAssignmentId() == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, session.getAssignmentId());
            }

            ps.setTimestamp(
                    4, Timestamp.valueOf(session.getStartTime())
            );
            ps.setTimestamp(
                    5, Timestamp.valueOf(session.getEndTime())
            );
            ps.setString(6, session.getNotes());

            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            session.setSessionId(keyHolder.getKey().longValue());
        }

        return session;
    }

    // Get all sessions belonging to a student
    public List<StudySession> findByStudentId(Long studentId) {
        String sql = """
                SELECT * FROM study_sessions
                WHERE student_id = ?
                ORDER BY start_time ASC
                """;

        return jdbcTemplate.query(
                sql, sessionRowMapper, studentId
        );
    }

    // Get one session belonging to a student
    public Optional<StudySession> findByIdAndStudentId(
            Long sessionId, Long studentId) {

        String sql = """
                SELECT * FROM study_sessions
                WHERE session_id = ? AND student_id = ?
                """;

        List<StudySession> results = jdbcTemplate.query(
                sql, sessionRowMapper, sessionId, studentId
        );

        return results.stream().findFirst();
    }

    // Update a study session
    public boolean update(StudySession session) {
        String sql = """
                UPDATE study_sessions
                SET subject_id = ?, assignment_id = ?,
                    start_time = ?, end_time = ?, notes = ?
                WHERE session_id = ? AND student_id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql,
                session.getSubjectId(),
                session.getAssignmentId(),
                Timestamp.valueOf(session.getStartTime()),
                Timestamp.valueOf(session.getEndTime()),
                session.getNotes(),
                session.getSessionId(),
                session.getStudentId()
        );

        return rowsAffected > 0;
    }

    // Delete a study session
    public boolean delete(Long sessionId, Long studentId) {
        String sql = """
                DELETE FROM study_sessions
                WHERE session_id = ? AND student_id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql, sessionId, studentId
        );

        return rowsAffected > 0;
    }
}
