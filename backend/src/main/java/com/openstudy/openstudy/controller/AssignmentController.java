
package com.openstudy.openstudy.controller;

import java.util.List;

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
import jakarta.validation.Valid;

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
            @Valid @RequestBody Assignment assignment) {
        assignment.setStudentId(studentId);
        Assignment saved = assignmentService.createAssignment(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Get all assignments for a student
    @GetMapping
    public ResponseEntity<?> getAssignments(
            @PathVariable Long studentId) {
        List<Assignment> assignments = assignmentService.getAssignments(studentId);
        return ResponseEntity.ok(assignments);
    }

    // Get one assignment
    @GetMapping("/{assignmentId}")
    public ResponseEntity<?> getAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        return assignmentService.getAssignment(assignmentId, studentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update an assignment
    @PutMapping("/{assignmentId}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody Assignment assignment) {
        assignment.setAssignmentId(assignmentId);
        assignment.setStudentId(studentId);
        boolean updated = assignmentService.updateAssignment(assignment);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("message", "Assignment updated successfully."));
    }

    // Delete an assignment
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        boolean deleted = assignmentService.deleteAssignment(assignmentId, studentId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("message", "Assignment deleted successfully."));
    }
}
