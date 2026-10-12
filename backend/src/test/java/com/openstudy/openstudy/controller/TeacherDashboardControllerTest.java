package com.openstudy.openstudy.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
class TeacherDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardRendersTeacherView() throws Exception {
        mockMvc.perform(get("/teacher/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(model().attributeExists("nav"));
    }

    @Test
    void teacherRootRedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/dashboard"));
    }

    @Test
    void placeholderIdentifiesOwningMember() throws Exception {
        mockMvc.perform(get("/teacher/coming-soon").param("module", "quizzes"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/coming-soon"))
                .andExpect(model().attribute("moduleOwner", "Member 2"))
                .andExpect(model().attribute("moduleName", "Quizzes"));
    }

    @Test
    void unrecognizedModuleNeverReachesTheViewModel() throws Exception {
        mockMvc.perform(get("/teacher/coming-soon").param("module", "<script>marker</script>"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("moduleKey", "module"))
                .andExpect(model().attribute("activePage", "module"));
    }
}
