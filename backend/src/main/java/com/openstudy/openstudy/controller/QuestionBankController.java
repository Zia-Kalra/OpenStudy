package com.openstudy.openstudy.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionResponse;
import com.openstudy.openstudy.repository.QuestionBankRepository;
import com.openstudy.openstudy.service.QuestionBankService;
import com.openstudy.openstudy.service.QuestionBankService.InvalidQuestionException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teacher/question-bank")
public class QuestionBankController {

    private final QuestionBankService service;

    public QuestionBankController(QuestionBankService service) {
        this.service = service;
    }

    @GetMapping
    public List<QuestionBankQuestionResponse> list(Principal principal) {
        return service.list(requirePrincipal(principal));
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(
            Principal principal,
            @Valid @RequestBody QuestionBankQuestionRequest request) {
        long questionId = service.createManual(requirePrincipal(principal), request);
        return ResponseEntity.created(URI.create("/api/teacher/question-bank/" + questionId))
                .body(Map.of("bankQuestionId", questionId));
    }

    @ExceptionHandler(QuestionBankRepository.TeacherNotFoundException.class)
    public ResponseEntity<Map<String, String>> teacherNotFound(QuestionBankRepository.TeacherNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidQuestionException.class)
    public ResponseEntity<Map<String, String>> invalidQuestion(InvalidQuestionException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return principal.getName();
    }
}