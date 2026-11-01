package com.openstudy.openstudy.error;

import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.TeacherNavLinksDTO;
import com.openstudy.openstudy.service.TeacherDashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

@Component
public class ErrorViewSupport {
    private static final TeacherNavLinksDTO FALLBACK_NAV = new TeacherNavLinksDTO(
            "/teacher/dashboard",
            "/teacher/coming-soon?module=question-bank",
            "/teacher/coming-soon?module=quizzes",
            "/teacher/coming-soon?module=assignments",
            "/teacher/coming-soon?module=submissions",
            "/teacher/analytics",
            "/teacher/coming-soon?module=profile",
            "/teacher/coming-soon?module=logout");
    private static final TeacherIdentityDTO FALLBACK_TEACHER =
            new TeacherIdentityDTO("unavailable", "Teacher", "", "Teacher", false);

    private final TeacherDashboardService dashboardService;

    public ErrorViewSupport(TeacherDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public ModelAndView page(ErrorPageContent content, String referenceId, String requestPath) {
        ModelAndView view = new ModelAndView("teacher/error");
        HttpStatus status = HttpStatus.resolve(content.status());
        view.setStatus(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        populate(view.getModel(), content, referenceId, requestPath);
        return view;
    }

    public void populate(Model model, ErrorPageContent content, String referenceId, String requestPath) {
        populate(model.asMap(), content, referenceId, requestPath);
    }

    public void populate(java.util.Map<String, Object> model, ErrorPageContent content, String referenceId,
            String requestPath) {
        model.put("errorContent", content);
        model.put("errorReferenceId", referenceId);
        model.put("errorPath", requestPath == null ? "" : requestPath);
        model.put("activePage", "dashboard");
        model.put("nav", nav());
        model.put("currentTeacher", teacher());
    }

    private TeacherNavLinksDTO nav() {
        try {
            return dashboardService.getNavLinks();
        } catch (RuntimeException ex) {
            return FALLBACK_NAV;
        }
    }

    private TeacherIdentityDTO teacher() {
        try {
            return dashboardService.getTeacherIdentity();
        } catch (RuntimeException ex) {
            return FALLBACK_TEACHER;
        }
    }
}
