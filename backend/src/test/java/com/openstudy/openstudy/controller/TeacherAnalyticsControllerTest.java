package com.openstudy.openstudy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
class TeacherAnalyticsControllerTest {
    @Autowired private MockMvc mvc;
    private MockHttpSession teacherSession;

    @BeforeEach
    void logInTeacher() throws Exception {
        var login = mvc.perform(post("/login")
                        .param("email", "teacher@example.com")
                        .param("password", "teacher123")
                        .param("role", "teacher"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/teacher/dashboard"))
                .andReturn();
        teacherSession = (MockHttpSession) login.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder teacherGet(String route) {
        return get(route).session(teacherSession);
    }

    @Test
    void teacherViewsHaveDistinctRoutesAndSharedLayout() throws Exception {
        for (String route : new String[] { "/teacher/analytics", "/teacher/analytics/students", "/teacher/analytics/students/S001",
                "/teacher/analytics/assessments", "/teacher/analytics/reports", "/teacher/analytics/reports/R-REG" }) {
            mvc.perform(teacherGet(route)).andExpect(status().isOk()).andExpect(view().name("teacher/analytics"))
                    .andExpect(model().attribute("activePage", "analytics"))
                    .andExpect(model().attributeExists("analytics", "nav", "currentTeacher"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    @Test
    void rejectsUnauthorizedPrincipalsFiltersAndDeepLinks() throws Exception {
        mvc.perform(teacherGet("/teacher/analytics").principal(() -> "student-1")).andExpect(status().isForbidden());
        mvc.perform(teacherGet("/teacher/analytics").param("classId", "CS-3C")).andExpect(status().isForbidden());
        mvc.perform(teacherGet("/teacher/analytics").param("subjectId", "PRIVATE")).andExpect(status().isForbidden());
        mvc.perform(teacherGet("/teacher/analytics/students/S999")).andExpect(status().isNotFound());
        mvc.perform(teacherGet("/teacher/analytics/students/S013").param("classId", "CS-2A")).andExpect(status().isNotFound());
        mvc.perform(teacherGet("/teacher/analytics/reports/R-ANOVA").param("classId", "CS-2A")).andExpect(status().isNotFound());
    }

    @Test
    void requestTeacherIdsCannotExpandServerScope() throws Exception {
        var result = mvc.perform(teacherGet("/teacher/analytics").param("teacherId", "OTHER-TEACHER").param("classIds", "CS-3C"))
                .andExpect(status().isOk()).andReturn();
        var dto = (TeacherAnalyticsDTO) result.getModelAndView().getModel().get("analytics");
        assertThat(dto.classes()).extracting(c -> c.id()).containsExactly("CS-2A", "CS-2B");
        assertThat(dto.students()).hasSize(20);
    }

    @Test
    void rejectsInvalidOrUnboundedFilters() throws Exception {
        mvc.perform(teacherGet("/teacher/analytics/students").param("search", "x".repeat(101))).andExpect(status().isBadRequest());
        mvc.perform(teacherGet("/teacher/analytics/students").param("sort", "SQL" )).andExpect(status().isBadRequest());
        mvc.perform(teacherGet("/teacher/analytics/reports").param("method", "shell" )).andExpect(status().isBadRequest());
    }
}
