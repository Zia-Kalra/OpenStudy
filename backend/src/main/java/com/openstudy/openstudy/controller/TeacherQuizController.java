
package com.openstudy.openstudy.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.QuizService;

@RestController
@Profile("!mock")
@RequestMapping("/api/teacher/quizzes")
public class TeacherQuizController {

    private final QuizService quizService;

    public TeacherQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects(HttpSession session) {
        long teacherId = requireTeacherId(session);
        return quizService.subjects(teacherId);
    }

    @GetMapping("/questions")
    public List<Map<String, Object>> questions(HttpSession session) {
        long teacherId = requireTeacherId(session);
        return quizService.approvedQuestions(teacherId);
    }

    @GetMapping
    public List<Map<String, Object>> quizzes(HttpSession session) {
        long teacherId = requireTeacherId(session);
        return quizService.quizzes(teacherId);
    }

    @PostMapping
    public Map<String, Object> create(
            HttpSession session,
            @RequestBody Map<String, Object> input) {

        long teacherId = requireTeacherId(session);

        // QuizService handles subjectName or an existing subjectId.
        long quizId = quizService.createQuiz(teacherId, input);

        return Map.of(
                "quizId", quizId,
                "message", "Quiz saved successfully.");
    }

    @PostMapping("/{quizId}/status")
    public Map<String, Object> updateStatus(
            HttpSession session,
            @PathVariable long quizId,
            @RequestBody Map<String, String> input) {

        long teacherId = requireTeacherId(session);
        String status = input.getOrDefault("status", "");

        quizService.updateStatus(teacherId, quizId, status);

        return Map.of(
                "quizId", quizId,
                "status", status,
                "message", "Quiz status updated.");
    }

    private long requireTeacherId(HttpSession session) {
        Object loggedIn = session == null
                ? null
                : session.getAttribute("loggedInUser");

        if (!(loggedIn instanceof User user)
                || user.getRole() == null
                || !"TEACHER".equalsIgnoreCase(user.getRole())
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "A logged-in teacher is required.");
        }

        return quizService.teacherId(user.getEmail());
    }
}
