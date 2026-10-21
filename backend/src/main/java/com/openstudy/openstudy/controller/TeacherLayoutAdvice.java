package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.service.TeacherDashboardService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {TeacherDashboardController.class, TeacherPlaceholderController.class,
        TeacherAnalyticsController.class})
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
    public Object currentTeacher() {
        return teacherDashboardService.getTeacherIdentity();
    }
}
