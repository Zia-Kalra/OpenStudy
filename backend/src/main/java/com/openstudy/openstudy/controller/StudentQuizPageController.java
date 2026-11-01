package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.StudentQuizService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Profile("!mock")
public class StudentQuizPageController {

    private final StudentQuizService quizzes;

    public StudentQuizPageController(StudentQuizService quizzes) {
        this.quizzes = quizzes;
    }

    @GetMapping("/student/quiz-attempts/{attemptId}")
    public String attempt(@PathVariable long attemptId, HttpSession session, Model model) {
        long studentId = studentId(session);
        var attempt = quizzes.attempt(studentId, attemptId);
        if (!"IN_PROGRESS".equals(attempt.get("status"))) {
            return "redirect:/student/quiz-attempts/" + attemptId + "/result";
        }
        addUser(model, session);
        model.addAttribute("activePage", "quizzes");
        model.addAttribute("attemptId", attemptId);
        return "student/quiz-attempt";
    }

    @GetMapping("/student/quiz-attempts/{attemptId}/submit")
    public String confirmSubmission(@PathVariable long attemptId, HttpSession session, Model model) {
        var attempt = quizzes.attempt(studentId(session), attemptId);
        if (!"IN_PROGRESS".equals(attempt.get("status"))) {
            return "redirect:/student/quiz-attempts/" + attemptId + "/result";
        }
        addUser(model, session);
        model.addAttribute("activePage", "quizzes");
        model.addAttribute("attemptId", attemptId);
        return "student/quiz-submit";
    }

    @GetMapping("/student/quiz-attempts/{attemptId}/result")
    public String result(@PathVariable long attemptId, HttpSession session, Model model) {
        addUser(model, session);
        model.addAttribute("activePage", "results");
        model.addAttribute("result", quizzes.result(studentId(session), attemptId));
        return "student/quiz-result";
    }

    private long studentId(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("loggedInUser");
        if (!(value instanceof User user) || user.getUserId() == null
                || user.getRole() == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "A logged-in student is required.");
        }
        return user.getUserId();
    }

    private void addUser(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        String name = user.getFullName() == null || user.getFullName().isBlank()
                ? "Student" : user.getFullName();
        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserName", name);
        model.addAttribute("currentUserEmail", user.getEmail());
        model.addAttribute("currentUserInitial", name.substring(0, 1).toUpperCase());
    }
}
