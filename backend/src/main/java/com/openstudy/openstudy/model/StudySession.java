
package com.openstudy.openstudy.model;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StudySession {

    private Long sessionId;
    @Positive(message = "Student ID must be positive.")
    private Long studentId;
    @Positive(message = "Subject ID must be positive.")
    private Long subjectId;
    @Positive(message = "Assignment ID must be positive.")
    private Long assignmentId;
    @NotNull(message = "Start time is required.")
    private LocalDateTime startTime;
    @NotNull(message = "End time is required.")
    private LocalDateTime endTime;
    private String notes;
    private LocalDateTime createdAt;

    // Default constructor
    public StudySession() {
    }

    // Getters and setters

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
