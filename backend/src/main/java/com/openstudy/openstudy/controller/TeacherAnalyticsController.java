package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.provider.analytics.TeacherAnalyticsAccessProvider;
import com.openstudy.openstudy.service.TeacherAnalyticsService;
import com.openstudy.openstudy.service.TeacherAnalyticsInsightsService;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class TeacherAnalyticsController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeacherAnalyticsController.class);
    private final TeacherAnalyticsService service;
    private final TeacherAnalyticsAccessProvider accessProvider;
    private final TeacherAnalyticsInsightsService insights;

    public TeacherAnalyticsController(TeacherAnalyticsService service, TeacherAnalyticsAccessProvider accessProvider) {
        this(service, accessProvider, null);
    }

    @Autowired
    public TeacherAnalyticsController(TeacherAnalyticsService service, TeacherAnalyticsAccessProvider accessProvider,
            TeacherAnalyticsInsightsService insights) {
        this.service = service;
        this.accessProvider = accessProvider;
        this.insights = insights;
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

    @GetMapping("/teacher/analytics/assessments/{assessmentId}")
    public String assessment(@PathVariable String assessmentId, @ModelAttribute TeacherAnalyticsQuery query,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        return render("assessments", query, null, null, identifier(assessmentId), request, response, model);
    }

    @PostMapping("/teacher/analytics/insights")
    public String classInsights(@ModelAttribute TeacherAnalyticsQuery query, @RequestParam String insightToken,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        return explain(query, null, insightToken, request, response, model);
    }

    @PostMapping("/teacher/analytics/students/{studentId}/insights")
    public String studentInsights(@PathVariable String studentId, @ModelAttribute TeacherAnalyticsQuery query,
            @RequestParam String insightToken, HttpServletRequest request, HttpServletResponse response, Model model) {
        return explain(query, identifier(studentId), insightToken, request, response, model);
    }

    private String explain(TeacherAnalyticsQuery query, String studentId, String token,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        var session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute("analyticsInsightToken");
        if (!(expected instanceof String saved) || token.length() > 128
                || !java.security.MessageDigest.isEqual(saved.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reload analytics before requesting insights.");
        }
        String view = render(studentId == null ? "overview" : "students", query, studentId, null, request, response, model);
        synchronized (session) {
            Object last = session.getAttribute("analyticsInsightRequestedAt");
            long now = System.currentTimeMillis();
            if (last instanceof Long time && now - time < 30000) {
                model.addAttribute("aiInsight", TeacherAnalyticsInsightsService.unavailable("Wait 30 seconds before requesting another AI summary."));
                return view;
            }
            session.setAttribute("analyticsInsightRequestedAt", now);
        }
        model.addAttribute("aiInsight", insights.explain((TeacherAnalyticsDTO) model.getAttribute("analytics"), studentId != null));
        return view;
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
        return render(section, query, studentId, reportId, null, request, response, model);
    }

    private String render(String section, TeacherAnalyticsQuery query, String studentId, String reportId, String assessmentId,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        var scope = accessProvider.resolveRequest(request);
        var analytics = assessmentId == null ? service.getAnalytics(scope, query, studentId, reportId)
                : service.getAnalytics(scope, query, studentId, reportId, assessmentId);
        model.addAttribute("analytics", analytics);
        model.addAttribute("currentTeacher", scope.teacher());
        model.addAttribute("activePage", "analytics");
        model.addAttribute("section", section);
        var session = request.getSession();
        if (session.getAttribute("analyticsInsightToken") == null) {
            session.setAttribute("analyticsInsightToken", java.util.UUID.randomUUID().toString());
        }
        model.addAttribute("analyticsInsightToken", session.getAttribute("analyticsInsightToken"));
        log.info("Teacher analytics rendered teacherId={} section={} subjects={} students={} assessments={} demo={}",
                scope.teacher().getTeacherId(), section, analytics.subjects().size(), analytics.overview().totalStudents(),
                analytics.assessments().size(), analytics.demo());
        return "teacher/analytics";
    }

    private String identifier(String value) {
        if (!value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid analytics identifier.");
        }
        return value;
    }
}
