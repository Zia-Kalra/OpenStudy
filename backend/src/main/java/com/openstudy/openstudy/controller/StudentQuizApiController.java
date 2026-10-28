package com.openstudy.openstudy.controller;

import java.util.List;
import java.util.Map;

import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.StudentQuizService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("!mock")
@RequestMapping("/api/student")
public class StudentQuizApiController {

    private static final Logger log = LoggerFactory.getLogger(StudentQuizApiController.class);

    private final StudentQuizService quizzes;

    public StudentQuizApiController(StudentQuizService quizzes) {
        this.quizzes = quizzes;
    }

    @GetMapping("/quizzes")
    public List<Map<String, Object>> available(HttpSession session, HttpServletRequest request) {
        long loggedInStudentId = studentId(session);
        String endpoint = request.getRequestURI();
        log.info("Student quiz listing requested endpoint={}", endpoint);
        try {
            List<Map<String, Object>> available = quizzes.availableQuizzes(loggedInStudentId);
            log.info("Student quiz listing completed endpoint={} returnedQuizCount={}",
                    endpoint, available.size());
            return available;
        } catch (DataAccessException exception) {
            log.error("Student quiz listing SQL failure endpoint={} exceptionType={}",
                    endpoint, exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    public Map<String, Object> start(@PathVariable long quizId, HttpSession session) {
        return quizzes.startOrResume(studentId(session), quizId);
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public Map<String, Object> attempt(@PathVariable long attemptId, HttpSession session) {
        return quizzes.attempt(studentId(session), attemptId);
    }

    @PutMapping("/quiz-attempts/{attemptId}/questions/{questionId}/answer")
    public ResponseEntity<Map<String, Object>> answer(
            @PathVariable long attemptId,
            @PathVariable long questionId,
            @RequestBody(required = false) Map<String, Object> input,
            HttpSession session) {
        List<Long> optionIds = selectedIds(input);
        Map<String, Object> response = quizzes.saveAnswer(studentId(session), attemptId, questionId, optionIds);
        return Boolean.TRUE.equals(response.get("expired"))
                ? ResponseEntity.status(HttpStatus.CONFLICT).body(response)
                : ResponseEntity.ok(response);
    }

    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public Map<String, Object> submit(@PathVariable long attemptId, HttpSession session) {
        return quizzes.submit(studentId(session), attemptId);
    }

    @GetMapping("/quiz-attempts/{attemptId}/result")
    public Map<String, Object> result(@PathVariable long attemptId, HttpSession session) {
        return quizzes.result(studentId(session), attemptId);
    }

    private long studentId(HttpSession session) {
        Object loggedIn = session == null ? null : session.getAttribute("loggedInUser");
        if (!(loggedIn instanceof User user)
                || user.getUserId() == null
                || user.getRole() == null
                || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "A logged-in student is required.");
        }
        return user.getUserId();
    }

    private List<Long> selectedIds(Map<String, Object> input) {
        if (input == null) return List.of();
        Object values = input.get("selectedOptionIds");
        if (values instanceof List<?> list) return list.stream().map(this::requiredId).toList();
        Long legacy = optionalId(input.get("selectedOptionId"));
        return legacy == null ? List.of() : List.of(legacy);
    }

    private long requiredId(Object value) {
        Long id = optionalId(value);
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected answer option is invalid.");
        return id;
    }

    private Long optionalId(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try {
            long id = Long.parseLong(value.toString());
            if (id < 1) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected answer option is invalid.");
        }
    }
}
