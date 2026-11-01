package com.openstudy.openstudy.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Profile("!mock")
public class TeacherQuestionBankPageController {

    @GetMapping("/teacher/question-bank")
    public String questionBank(Model model) {
        model.addAttribute("activePage", "question-bank");
        return "teacher/question-bank";
    }

    @GetMapping("/teacher/question-bank/create")
    public String createQuestion(
            @RequestParam(required = false) String saved,
            Model model) {
        model.addAttribute("activePage", "question-bank");
        model.addAttribute("saved", "true".equals(saved));
        return "teacher/question-create";
    }
    
    @GetMapping("/teacher/question-bank/ai")
    public String generateAiPage(Model model) {
        model.addAttribute("activePage", "question-bank");
        return "teacher/question-ai";
    }
}
