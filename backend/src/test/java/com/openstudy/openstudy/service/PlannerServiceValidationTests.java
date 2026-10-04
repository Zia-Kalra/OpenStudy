package com.openstudy.openstudy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openstudy.openstudy.exception.ResourceNotFoundException;
import com.openstudy.openstudy.model.Assignment;
import com.openstudy.openstudy.model.StudySession;
import com.openstudy.openstudy.repository.AssignmentRepository;
import com.openstudy.openstudy.repository.PlannerReferenceRepository;
import com.openstudy.openstudy.repository.StudySessionRepository;

class PlannerServiceValidationTests {

    private AssignmentRepository assignments;
    private StudySessionRepository sessions;
    private PlannerReferenceRepository references;
    private AssignmentService assignmentService;
    private StudySessionService sessionService;

    @BeforeEach
    void setUp() {
        assignments = mock(AssignmentRepository.class);
        sessions = mock(StudySessionRepository.class);
        references = mock(PlannerReferenceRepository.class);
        assignmentService = new AssignmentService(assignments, references);
        sessionService = new StudySessionService(sessions, references);
        when(references.studentExists(1L)).thenReturn(true);
        when(references.studentExists(2L)).thenReturn(true);
    }

    @Test
    void createsAndRetrievesValidAssignmentForItsStudent() {
        Assignment assignment = validAssignment(1L);
        when(assignments.save(assignment)).thenReturn(assignment);
        when(assignments.findByIdAndStudentId(9L, 1L)).thenReturn(Optional.of(assignment));

        assertEquals(assignment, assignmentService.createAssignment(assignment));
        assertEquals(Optional.of(assignment), assignmentService.getAssignment(9L, 1L));
        verify(assignments).findByIdAndStudentId(9L, 1L);
    }

    @Test
    void rejectsInvalidAssignmentFields() {
        Assignment invalidTitle = validAssignment(1L);
        invalidTitle.setTitle("  ");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(invalidTitle));

        Assignment invalidPriority = validAssignment(1L);
        invalidPriority.setPriority("URGENT");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(invalidPriority));

        Assignment invalidStatus = validAssignment(1L);
        invalidStatus.setStatus("DONE");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(invalidStatus));
    }

    @Test
    void scopesAssignmentReadsAndWritesToStudent() {
        when(assignments.findByIdAndStudentId(9L, 2L)).thenReturn(Optional.empty());
        when(assignments.update(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(assignments.delete(9L, 2L)).thenReturn(false);

        assertTrue(assignmentService.getAssignment(9L, 2L).isEmpty());
        Assignment update = validAssignment(2L);
        update.setAssignmentId(9L);
        assertEquals(false, assignmentService.updateAssignment(update));
        assertEquals(false, assignmentService.deleteAssignment(9L, 2L));
        verify(assignments).findByIdAndStudentId(9L, 2L);
        verify(assignments).delete(9L, 2L);
    }

    @Test
    void rejectsSubjectAndAssignmentOwnedByAnotherStudent() {
        Assignment assignment = validAssignment(1L);
        assignment.setSubjectId(42L);
        when(references.subjectBelongsToStudent(42L, 1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createAssignment(assignment));

        StudySession session = validSession(1L);
        session.setAssignmentId(73L);
        when(references.assignmentBelongsToStudent(73L, 1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> sessionService.createSession(session));

        StudySession invalidSubject = validSession(1L);
        invalidSubject.setSubjectId(42L);
        assertThrows(ResourceNotFoundException.class, () -> sessionService.createSession(invalidSubject));
    }

    @Test
    void rejectsInvalidSessionTimesAndRequiresAnExistingStudent() {
        StudySession session = validSession(1L);
        session.setEndTime(session.getStartTime());
        assertThrows(IllegalArgumentException.class, () -> sessionService.createSession(session));

        StudySession missingStudent = validSession(3L);
        assertThrows(ResourceNotFoundException.class, () -> sessionService.createSession(missingStudent));
    }

    @Test
    void createsRetrievesAndDeletesSessionUsingStudentScopedRepositoryOperations() {
        StudySession session = validSession(1L);
        when(sessions.save(session)).thenReturn(session);
        when(sessions.findByIdAndStudentId(12L, 1L)).thenReturn(Optional.of(session));
        when(sessions.update(session)).thenReturn(true);
        when(sessions.delete(12L, 1L)).thenReturn(true);
        session.setSessionId(12L);

        assertEquals(session, sessionService.createSession(session));
        assertEquals(Optional.of(session), sessionService.getSession(12L, 1L));
        assertTrue(sessionService.updateSession(session));
        assertTrue(sessionService.deleteSession(12L, 1L));
        verify(sessions).findByIdAndStudentId(12L, 1L);
        verify(sessions).update(session);
        verify(sessions).delete(12L, 1L);
    }

    private Assignment validAssignment(Long studentId) {
        Assignment assignment = new Assignment();
        assignment.setStudentId(studentId);
        assignment.setTitle("Read chapter");
        assignment.setDueDate(LocalDateTime.of(2026, 11, 1, 12, 0));
        assignment.setPriority("MEDIUM");
        assignment.setStatus("PENDING");
        return assignment;
    }

    private StudySession validSession(Long studentId) {
        StudySession session = new StudySession();
        session.setStudentId(studentId);
        session.setStartTime(LocalDateTime.of(2026, 11, 1, 10, 0));
        session.setEndTime(LocalDateTime.of(2026, 11, 1, 11, 0));
        return session;
    }
}
