
package com.openstudy.openstudy.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.openstudy.openstudy.model.Assignment;
import com.openstudy.openstudy.repository.AssignmentRepository;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    private static final Set<String> VALID_PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH");

    private static final Set<String> VALID_STATUSES =
            Set.of("PENDING", "IN_PROGRESS", "COMPLETED");

    public AssignmentService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    // Validate and create an assignment
    public Assignment createAssignment(Assignment assignment) {
        validateAssignment(assignment);

        if (assignment.getStudentId() == null) {
            throw new IllegalArgumentException(
                    "Student ID is required."
            );
        }

        if (assignment.getStudentId() <= 0) {
            throw new IllegalArgumentException(
                    "Student ID must be positive."
            );
        }

        return assignmentRepository.save(assignment);
    }

    // Get all assignments belonging to a student
    public List<Assignment> getAssignments(Long studentId) {
        validateStudentId(studentId);

        return assignmentRepository.findByStudentId(studentId);
    }

    // Get one assignment belonging to a student
    public Optional<Assignment> getAssignment(
            Long assignmentId, Long studentId) {

        validateStudentId(studentId);
        validateAssignmentId(assignmentId);

        return assignmentRepository.findByIdAndStudentId(
                assignmentId, studentId
        );
    }

    // Validate and update an assignment
    public boolean updateAssignment(Assignment assignment) {
        validateAssignment(assignment);
        validateAssignmentId(assignment.getAssignmentId());
        validateStudentId(assignment.getStudentId());

        return assignmentRepository.update(assignment);
    }

    // Delete an assignment belonging to a student
    public boolean deleteAssignment(
            Long assignmentId, Long studentId) {

        validateAssignmentId(assignmentId);
        validateStudentId(studentId);

        return assignmentRepository.delete(
                assignmentId, studentId
        );
    }

    // Common assignment validation
    private void validateAssignment(Assignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException(
                    "Assignment data is required."
            );
        }

        if (assignment.getTitle() == null
                || assignment.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Assignment title is required."
            );
        }

        if (assignment.getTitle().length() > 150) {
            throw new IllegalArgumentException(
                    "Assignment title cannot exceed 150 characters."
            );
        }

        if (assignment.getDueDate() == null) {
            throw new IllegalArgumentException(
                    "Due date is required."
            );
        }

        if (assignment.getPriority() == null
                || !VALID_PRIORITIES.contains(
                        assignment.getPriority())) {
            throw new IllegalArgumentException(
                    "Priority must be LOW, MEDIUM, or HIGH."
            );
        }

        if (assignment.getStatus() == null
                || !VALID_STATUSES.contains(
                        assignment.getStatus())) {
            throw new IllegalArgumentException(
                    "Status must be PENDING, IN_PROGRESS, or COMPLETED."
            );
        }
    }

    private void validateStudentId(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException(
                    "A valid student ID is required."
            );
        }
    }

    private void validateAssignmentId(Long assignmentId) {
        if (assignmentId == null || assignmentId <= 0) {
            throw new IllegalArgumentException(
                    "A valid assignment ID is required."
            );
        }
    }
}
