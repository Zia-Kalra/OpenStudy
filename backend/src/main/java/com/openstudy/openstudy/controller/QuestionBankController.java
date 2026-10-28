package com.openstudy.openstudy.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionResponse;
import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.repository.QuestionBankRepository;
import com.openstudy.openstudy.service.QuestionBankService;
import com.openstudy.openstudy.service.QuestionBankService.InvalidQuestionException;

@RestController
@Profile("!mock")
@RequestMapping("/api/teacher/question-bank")
public class QuestionBankController {

    private final QuestionBankService service;

    public QuestionBankController(QuestionBankService service, QuestionBankRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    private final QuestionBankRepository repository;

    @GetMapping("/subjects") public List<Map<String,Object>> subjects(HttpSession session) {
        return repository.findTeacherSubjects(repository.findActiveTeacherId(requireTeacherEmail(session)));
    }

    @PutMapping("/{id}") public ResponseEntity<Void> update(HttpSession session, @PathVariable long id, @Valid @RequestBody QuestionBankQuestionRequest request) {
        service.update(requireTeacherEmail(session), id, request); return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status") public ResponseEntity<Void> status(HttpSession session, @PathVariable long id, @RequestBody Map<String,String> body) {
        service.updateStatus(requireTeacherEmail(session), id, body.getOrDefault("status", "")); return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(HttpSession session, @PathVariable long id) {
        service.delete(requireTeacherEmail(session), id); return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<QuestionBankQuestionResponse> list(HttpSession session) {
        return service.list(requireTeacherEmail(session));
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(
            HttpSession session,
            @Valid @RequestBody QuestionBankQuestionRequest request) {

        long questionId = service.createManual(
                requireTeacherEmail(session), request);

        return ResponseEntity
                .created(URI.create("/api/teacher/question-bank/" + questionId))
                .body(Map.of("bankQuestionId", questionId));
    }

    /**
     * Saves an AI-generated question after the teacher has reviewed it.
     */
    @PostMapping("/ai")
    public ResponseEntity<Map<String, Long>> saveReviewedAi(
            HttpSession session,
            @Valid @RequestBody QuestionBankQuestionRequest request) {

        long questionId = service.createReviewedAi(
                requireTeacherEmail(session), request);

        return ResponseEntity
                .created(URI.create("/api/teacher/question-bank/" + questionId))
                .body(Map.of("bankQuestionId", questionId));
    }

    @ExceptionHandler(QuestionBankRepository.TeacherNotFoundException.class)
    public ResponseEntity<Map<String, String>> teacherNotFound(
            QuestionBankRepository.TeacherNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidQuestionException.class)
    public ResponseEntity<Map<String, String>> invalidQuestion(
            InvalidQuestionException ex) {

        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(QuestionBankRepository.QuestionNotFoundException.class)
    public ResponseEntity<Map<String,String>> notFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found")); }
    @ExceptionHandler(QuestionBankRepository.QuestionInUseException.class)
    public ResponseEntity<Map<String,String>> inUse() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "This question is already used in a quiz and cannot be deleted")); }

    private String requireTeacherEmail(HttpSession session) {
        Object loggedIn = session == null
                ? null
                : session.getAttribute("loggedInUser");

        if (!(loggedIn instanceof User user)
                || user.getRole() == null
                || !"teacher".equalsIgnoreCase(user.getRole())
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "A logged-in teacher is required");
        }

        return user.getEmail();
    }
}
