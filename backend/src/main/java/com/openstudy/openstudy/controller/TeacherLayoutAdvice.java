package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.TeacherDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {TeacherDashboardController.class, TeacherPlaceholderController.class,
        TeacherAnalyticsController.class, TeacherQuestionBankPageController.class, TeacherQuizPageController.class,
        TeacherProfileController.class})
public class TeacherLayoutAdvice {

    private final TeacherDashboardService teacherDashboardService;

    public TeacherLayoutAdvice(TeacherDashboardService teacherDashboardService) {
        this.teacherDashboardService = teacherDashboardService;
    }

    @ModelAttribute("nav")
    public Object nav() {
        return teacherDashboardService.getNavLinks();
    }

    @ModelAttribute("currentTeacher")
    public Object currentTeacher(HttpServletRequest request) {
        User sessionUser = request.getSession(false) == null
                ? null
                : (User) request.getSession(false).getAttribute("loggedInUser");

        if (sessionUser != null && "teacher".equalsIgnoreCase(sessionUser.getRole())) {
            return new TeacherIdentityDTO(
                    sessionUser.getUserId() == null ? "teacher-session" : String.valueOf(sessionUser.getUserId()),
                    sessionUser.getFullName() == null || sessionUser.getFullName().isBlank()
                            ? "Teacher User"
                            : sessionUser.getFullName(),
                    sessionUser.getEmail() == null ? "teacher@example.com" : sessionUser.getEmail(),
                    sessionUser.getRole(),
                    false);
        }

        return teacherDashboardService.getTeacherIdentity();
    }
}
