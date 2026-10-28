package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.service.TeacherDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;

    public TeacherDashboardController(TeacherDashboardService teacherDashboardService) {
        this.teacherDashboardService = teacherDashboardService;
    }

    @GetMapping("/teacher")
    public String teacherRoot() {
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/teacher/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", teacherDashboardService.getDashboard());
        model.addAttribute("activePage", "dashboard");
        return "teacher/dashboard";
    }

    @GetMapping("/teacher/assignments")
    public String assignments(Model model) {
        model.addAttribute("activePage", "assignments");
        model.addAttribute("assignmentUiRole", "TEACHER");
        return "teacher/assignments";
    }

    @GetMapping("/teacher/submissions")
    public String submissions(Model model) {
        model.addAttribute("activePage", "submissions");
        return "teacher/submissions";
    }

    @GetMapping("/teacher/assignments/create")
    public String createAssignment(@RequestParam(required = false) Long assignmentId, Model model) {
        model.addAttribute("activePage", "assignments");
        model.addAttribute("assignmentUiRole", "TEACHER");
        model.addAttribute("assignmentId", assignmentId);
        return "teacher/assignment-create";
    }
}
