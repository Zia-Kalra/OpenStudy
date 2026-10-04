
package com.openstudy.openstudy.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.openstudy.openstudy.model.StudySession;
import com.openstudy.openstudy.service.StudySessionService;

@RestController
@RequestMapping("/api/students/{studentId}/study-sessions")
public class StudySessionController {

    private final StudySessionService sessionService;

    public StudySessionController(StudySessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<?> createSession(
            @PathVariable Long studentId,
            @Valid @RequestBody StudySession session) {
        session.setStudentId(studentId);
        StudySession created = sessionService.createSession(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<StudySession>> getSessions(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(sessionService.getSessions(studentId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable Long studentId,
            @PathVariable Long sessionId) {
        Optional<StudySession> session =
                sessionService.getSession(sessionId, studentId);

        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(session.get());
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long studentId,
            @PathVariable Long sessionId,
            @Valid @RequestBody StudySession session) {
        session.setStudentId(studentId);
        session.setSessionId(sessionId);
        boolean updated = sessionService.updateSession(session);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("message", "Study session updated successfully"));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable Long studentId,
            @PathVariable Long sessionId) {
        boolean deleted =
                sessionService.deleteSession(sessionId, studentId);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                Map.of("message", "Study session deleted successfully"));
    }
}
