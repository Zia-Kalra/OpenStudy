package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.provider.analytics.TeacherAnalyticsAccessProvider;
import com.openstudy.openstudy.service.TeacherAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class TeacherAnalyticsController {
    private final TeacherAnalyticsService service;
    private final TeacherAnalyticsAccessProvider accessProvider;

    public TeacherAnalyticsController(TeacherAnalyticsService service, TeacherAnalyticsAccessProvider accessProvider) {
        this.service = service;
        this.accessProvider = accessProvider;
    }

    @GetMapping("/teacher/analytics")
    public String overview(@ModelAttribute TeacherAnalyticsQuery query, HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("overview", query, null, null, request, response, model);
    }

    @GetMapping("/teacher/analytics/students")
    public String students(@ModelAttribute TeacherAnalyticsQuery query, HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("students", query, null, null, request, response, model);
    }

    @GetMapping("/teacher/analytics/students/{studentId}")
    public String student(@PathVariable String studentId, @ModelAttribute TeacherAnalyticsQuery query,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("students", query, identifier(studentId), null, request, response, model);
    }

    @GetMapping("/teacher/analytics/assessments")
    public String assessments(@ModelAttribute TeacherAnalyticsQuery query, HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("assessments", query, null, null, request, response, model);
    }

    @GetMapping("/teacher/analytics/reports")
    public String reports(@ModelAttribute TeacherAnalyticsQuery query, HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("reports", query, null, null, request, response, model);
    }

    @GetMapping("/teacher/analytics/reports/{reportId}")
    public String report(@PathVariable String reportId, @ModelAttribute TeacherAnalyticsQuery query,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("reports", query, null, identifier(reportId), request, response, model);
    }

    private String render(String section, TeacherAnalyticsQuery query, String studentId, String reportId,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        var scope = accessProvider.resolve(request.getUserPrincipal());
        var analytics = service.getAnalytics(scope, query, studentId, reportId);
        model.addAttribute("analytics", analytics);
        model.addAttribute("currentTeacher", scope.teacher());
        model.addAttribute("activePage", "analytics");
        model.addAttribute("section", section);
        return "teacher/analytics";
    }

    private String identifier(String value) {
        if (!value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid analytics identifier.");
        }
        return value;
    }
}
