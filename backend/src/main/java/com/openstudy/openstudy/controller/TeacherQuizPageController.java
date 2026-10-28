package com.openstudy.openstudy.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("!mock")
public class TeacherQuizPageController {

    @GetMapping("/teacher/quizzes")
    public String manageQuizzes(Model model) {
        model.addAttribute("activePage", "quizzes");
        return "teacher/quizzes";
    }

    @GetMapping("/teacher/quizzes/create")
    public String createQuiz(Model model) {
        model.addAttribute("activePage", "quizzes");
        return "teacher/quiz-create";
    }
}
