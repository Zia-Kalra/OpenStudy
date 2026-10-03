
package com.openstudy.openstudy.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openstudy.openstudy.model.Assignment;
import com.openstudy.openstudy.service.AssignmentService;

@RestController
@RequestMapping("/api/students/{studentId}/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // Create a new assignment
    @PostMapping
    public ResponseEntity<?> createAssignment(
            @PathVariable Long studentId,
            @RequestBody Assignment assignment) {
        try {
            assignment.setStudentId(studentId);

            Assignment saved =
                    assignmentService.createAssignment(assignment);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get all assignments for a student
    @GetMapping
    public ResponseEntity<?> getAssignments(
            @PathVariable Long studentId) {
        try {
            List<Assignment> assignments =
                    assignmentService.getAssignments(studentId);

            return ResponseEntity.ok(assignments);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get one assignment
    @GetMapping("/{assignmentId}")
    public ResponseEntity<?> getAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        try {
            return assignmentService
                    .getAssignment(assignmentId, studentId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(
                            HttpStatus.NOT_FOUND).body(
                            Map.of("error", "Assignment not found.")));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Update an assignment
    @PutMapping("/{assignmentId}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId,
            @RequestBody Assignment assignment) {
        try {
            assignment.setAssignmentId(assignmentId);
            assignment.setStudentId(studentId);

            boolean updated =
                    assignmentService.updateAssignment(assignment);

            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Assignment not found."));
            }

            return ResponseEntity.ok(
                    Map.of("message", "Assignment updated successfully."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete an assignment
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        try {
            boolean deleted = assignmentService
                    .deleteAssignment(assignmentId, studentId);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Assignment not found."));
            }

            return ResponseEntity.ok(
                    Map.of("message", "Assignment deleted successfully."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
