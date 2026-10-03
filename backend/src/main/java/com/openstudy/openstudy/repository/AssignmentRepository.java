
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

import com.openstudy.openstudy.model.Assignment;

@Repository
public class AssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Converts a database row into an Assignment object
    private final RowMapper<Assignment> assignmentRowMapper =
            (rs, rowNum) -> {
                Assignment assignment = new Assignment();

                assignment.setAssignmentId(rs.getLong("assignment_id"));
                assignment.setStudentId(rs.getLong("student_id"));

                long subjectId = rs.getLong("subject_id");
                assignment.setSubjectId(
                        rs.wasNull() ? null : subjectId
                );

                assignment.setTitle(rs.getString("title"));
                assignment.setDescription(rs.getString("description"));

                Timestamp dueDate = rs.getTimestamp("due_date");
                assignment.setDueDate(dueDate.toLocalDateTime());

                assignment.setPriority(rs.getString("priority"));
                assignment.setStatus(rs.getString("status"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    assignment.setCreatedAt(createdAt.toLocalDateTime());
                }

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    assignment.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                return assignment;
            };

    // Create a new assignment
    public Assignment save(Assignment assignment) {
        String sql = """
                INSERT INTO assignments
                (student_id, subject_id, title, description,
                 due_date, priority, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setLong(1, assignment.getStudentId());

            if (assignment.getSubjectId() == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, assignment.getSubjectId());
            }

            ps.setString(3, assignment.getTitle());
            ps.setString(4, assignment.getDescription());
            ps.setTimestamp(
                    5, Timestamp.valueOf(assignment.getDueDate())
            );
            ps.setString(6, assignment.getPriority());
            ps.setString(7, assignment.getStatus());

            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            assignment.setAssignmentId(keyHolder.getKey().longValue());
        }

        return assignment;
    }

    // Get all assignments belonging to one student
    public List<Assignment> findByStudentId(Long studentId) {
        String sql = """
                SELECT * FROM assignments
                WHERE student_id = ?
                ORDER BY due_date ASC
                """;

        return jdbcTemplate.query(
                sql, assignmentRowMapper, studentId
        );
    }

    // Get one assignment, provided it belongs to the student
    public Optional<Assignment> findByIdAndStudentId(
            Long assignmentId, Long studentId) {

        String sql = """
                SELECT * FROM assignments
                WHERE assignment_id = ? AND student_id = ?
                """;

        List<Assignment> results = jdbcTemplate.query(
                sql, assignmentRowMapper, assignmentId, studentId
        );

        return results.stream().findFirst();
    }

    // Update an existing assignment
    public boolean update(Assignment assignment) {
        String sql = """
                UPDATE assignments
                SET subject_id = ?, title = ?, description = ?,
                    due_date = ?, priority = ?, status = ?
                WHERE assignment_id = ? AND student_id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql,
                assignment.getSubjectId(),
                assignment.getTitle(),
                assignment.getDescription(),
                Timestamp.valueOf(assignment.getDueDate()),
                assignment.getPriority(),
                assignment.getStatus(),
                assignment.getAssignmentId(),
                assignment.getStudentId()
        );

        return rowsAffected > 0;
    }

    // Delete an assignment belonging to the student
    public boolean delete(Long assignmentId, Long studentId) {
        String sql = """
                DELETE FROM assignments
                WHERE assignment_id = ? AND student_id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql, assignmentId, studentId
        );

        return rowsAffected > 0;
    }
}
