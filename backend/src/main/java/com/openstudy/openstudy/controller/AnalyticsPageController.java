package com.openstudy.openstudy.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Member 4 student analytics page. It is an HTML shell only: every number is fetched from /api/analytics/**,
 * which enforces login, role and ownership on the server. The teacher analytics pages are served by
 * {@link TeacherAnalyticsController}.
 */
@Controller
@Profile("!mock")
public class AnalyticsPageController {

    /** Student "Performance" page (sidebar entry from the supplied design). */
    @GetMapping("/student/analytics")
    public String studentPerformance(Model model) {
        model.addAttribute("activePage", "performance");
        return "student/analytics";
    }
}
