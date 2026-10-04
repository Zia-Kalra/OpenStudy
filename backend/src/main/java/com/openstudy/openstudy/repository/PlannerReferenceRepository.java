package com.openstudy.openstudy.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlannerReferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlannerReferenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean studentExists(Long studentId) {
        return exists("SELECT EXISTS(SELECT 1 FROM students WHERE student_id = ?)", studentId);
    }

    public boolean subjectBelongsToStudent(Long subjectId, Long studentId) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM subjects WHERE subject_id = ? AND student_id = ?)",
                Boolean.class, subjectId, studentId);
    }

    public boolean assignmentBelongsToStudent(Long assignmentId, Long studentId) {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM assignments WHERE assignment_id = ? AND student_id = ?)",
                Boolean.class, assignmentId, studentId);
    }

    private boolean exists(String sql, Long id) {
        return jdbcTemplate.queryForObject(sql, Boolean.class, id);
    }
}
