package com.openstudy.openstudy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import com.openstudy.openstudy.model.User;

@Controller
public class AssignmentPageController {
    @GetMapping({"/assignments", "/assignments/"})
    public String assignments(HttpSession session, Model model) {
        Object role = session.getAttribute("role");
        Object userRole = session.getAttribute("userRole");
        Object loggedInUser = session.getAttribute("loggedInUser");
        String currentRole = role != null ? role.toString() : userRole == null ? "" : userRole.toString();
        if (loggedInUser instanceof User user && user.getRole() != null) {
            currentRole = user.getRole();
        }
        model.addAttribute("assignmentUiRole", currentRole.toUpperCase(java.util.Locale.ROOT));
        return "assignments";
    }
}
