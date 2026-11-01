
package com.openstudy.openstudy.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openstudy.openstudy.dto.GeneratedMCQDTO;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.ai.AIService;

@RestController
@Profile("!mock")
@RequestMapping("/api/teacher/mcq-ai")
public class TeacherMcqAiController {

    private final AIService aiService;

    public TeacherMcqAiController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    public List<GeneratedMCQDTO> generate(
            HttpSession session,
            @Valid @RequestBody MCQGenerationRequest request) {
        requireTeacher(session);
        return aiService.generateMCQs(request);
    }

    private void requireTeacher(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("loggedInUser");
        if (!(value instanceof User user)
                || user.getRole() == null
                || !"teacher".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "A logged-in teacher is required");
        }
    }
}
