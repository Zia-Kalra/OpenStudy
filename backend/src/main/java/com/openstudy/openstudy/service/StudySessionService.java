
package com.openstudy.openstudy.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.openstudy.openstudy.model.StudySession;
import com.openstudy.openstudy.repository.StudySessionRepository;

@Service
public class StudySessionService {

    private final StudySessionRepository sessionRepository;

    public StudySessionService(
            StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // Create a study session
    public StudySession createSession(StudySession session) {
        validateSession(session);
        validateStudentId(session.getStudentId());

        return sessionRepository.save(session);
    }

    // Get all study sessions for a student
    public List<StudySession> getSessions(Long studentId) {
        validateStudentId(studentId);

        return sessionRepository.findByStudentId(studentId);
    }

    // Get one study session
    public Optional<StudySession> getSession(
            Long sessionId, Long studentId) {
        validateSessionId(sessionId);
        validateStudentId(studentId);

        return sessionRepository.findByIdAndStudentId(
                sessionId, studentId);
    }

    // Update a study session
    public boolean updateSession(StudySession session) {
        validateSession(session);
        validateSessionId(session.getSessionId());
        validateStudentId(session.getStudentId());

        return sessionRepository.update(session);
    }

    // Delete a study session
    public boolean deleteSession(
            Long sessionId, Long studentId) {
        validateSessionId(sessionId);
        validateStudentId(studentId);

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
}
