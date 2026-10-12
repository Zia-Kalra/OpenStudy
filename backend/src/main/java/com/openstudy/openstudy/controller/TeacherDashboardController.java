package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.service.TeacherDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}
