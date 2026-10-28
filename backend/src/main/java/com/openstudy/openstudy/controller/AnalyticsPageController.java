package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.service.TeacherDashboardService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Member 4 pages. They are HTML shells only: every number is fetched from /api/analytics/**, which
 * enforces login, role and ownership on the server.
 */
@Controller
@Profile("!mock")
public class AnalyticsPageController {

    private final TeacherDashboardService teacherDashboardService;

    public AnalyticsPageController(TeacherDashboardService teacherDashboardService) {
        this.teacherDashboardService = teacherDashboardService;
    }

    /** Student "Performance" page (sidebar entry from the supplied design). */
    @GetMapping("/student/analytics")
    public String studentPerformance(Model model) {
        model.addAttribute("activePage", "performance");
        return "student/analytics";
    }

    /** Teacher "Analytics" page. Reuses Member 5's sidebar/navbar fragments and model attributes. */
    @GetMapping("/teacher/analytics")
    public String teacherAnalytics(Model model) {
        model.addAttribute("nav", teacherDashboardService.getNavLinks());
        model.addAttribute("currentTeacher", teacherDashboardService.getTeacherIdentity());
        model.addAttribute("activePage", "analytics");
        return "teacher/analytics";
    }
}
