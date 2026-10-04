
package com.openstudy.openstudy.service;

import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.openstudy.openstudy.model.StudySession;
import com.openstudy.openstudy.repository.StudySessionRepository;
import com.openstudy.openstudy.repository.PlannerReferenceRepository;
import com.openstudy.openstudy.exception.ResourceNotFoundException;

@Service
public class StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final PlannerReferenceRepository references;

    public StudySessionService(
            StudySessionRepository sessionRepository,
            PlannerReferenceRepository references) {
        this.sessionRepository = sessionRepository;
        this.references = references;
    }

    // Create a study session
    public StudySession createSession(StudySession session) {
        validateSession(session);
        validateStudentId(session.getStudentId());
        requireStudent(session.getStudentId());
        validateReferences(session);

        return sessionRepository.save(session);
    }

    // Get all study sessions for a student
    public List<StudySession> getSessions(Long studentId) {
        validateStudentId(studentId);
        requireStudent(studentId);

        return sessionRepository.findByStudentId(studentId);
    }

    // Get one study session
    public Optional<StudySession> getSession(
            Long sessionId, Long studentId) {
        validateSessionId(sessionId);
        validateStudentId(studentId);
        requireStudent(studentId);

        return sessionRepository.findByIdAndStudentId(
                sessionId, studentId);
    }

    // Update a study session
    public boolean updateSession(StudySession session) {
        validateSession(session);
        validateSessionId(session.getSessionId());
        validateStudentId(session.getStudentId());
        requireStudent(session.getStudentId());
        validateReferences(session);

        return sessionRepository.update(session);
    }

    // Delete a study session
    public boolean deleteSession(
            Long sessionId, Long studentId) {
        validateSessionId(sessionId);
        validateStudentId(studentId);
        requireStudent(studentId);

        return sessionRepository.delete(sessionId, studentId);
    }

    // Validate session data
    private void validateSession(StudySession session) {
        if (session == null) {
            throw new IllegalArgumentException(
                    "Study session data is required.");
        }

        if (session.getStartTime() == null
                || session.getEndTime() == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required.");
        }

        if (!session.getEndTime().isAfter(session.getStartTime())) {
            throw new IllegalArgumentException(
                    "End time must be after start time.");
        }

        if (session.getNotes() != null
                && session.getNotes().getBytes(StandardCharsets.UTF_8).length > 65_535) {
            throw new IllegalArgumentException("Notes exceed the database text limit.");
        }
    }

    private void validateStudentId(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException(
                    "A valid student ID is required.");
        }
    }

    private void validateSessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException(
                    "A valid session ID is required.");
        }
    }

    private void requireStudent(Long studentId) {
        if (!references.studentExists(studentId)) {
            throw new ResourceNotFoundException("Student not found.");
        }
    }

    private void validateReferences(StudySession session) {
        if (session.getSubjectId() != null
                && !references.subjectBelongsToStudent(session.getSubjectId(), session.getStudentId())) {
            throw new ResourceNotFoundException("Subject not found for this student.");
        }
        if (session.getAssignmentId() != null
                && !references.assignmentBelongsToStudent(session.getAssignmentId(), session.getStudentId())) {
            throw new ResourceNotFoundException("Assignment not found for this student.");
        }
    }
}
