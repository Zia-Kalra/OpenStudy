package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.MockJdbcTestConfiguration;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.openstudy.openstudy.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")

@Import(MockJdbcTestConfiguration.class)

class TeacherDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardRendersTeacherView() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(model().attributeExists("nav"));
    }

    @Test
    void teacherRootRedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/teacher").session(teacherSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/dashboard"));
    }

    @Test
    void teacherLogoutNavigationUsesActualLogoutRoute() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("nav", org.hamcrest.Matchers.hasProperty("logout", is("/logout"))));
    }

    @Test
    void logoutPlaceholderRedirectsToRealLogoutEndpoint() throws Exception {
        mockMvc.perform(get("/teacher/coming-soon").param("module", "logout").session(teacherSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout"));
    }

    @Test
    void placeholderIdentifiesOwningMember() throws Exception {
        mockMvc.perform(get("/teacher/coming-soon").param("module", "quizzes").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/coming-soon"))
                .andExpect(model().attribute("moduleOwner", "Member 2"))
                .andExpect(model().attribute("moduleName", "Quizzes"));
    }

    @Test
    void unrecognizedModuleNeverReachesTheViewModel() throws Exception {
        mockMvc.perform(get("/teacher/coming-soon").param("module", "<script>marker</script>").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("moduleKey", "module"))
                .andExpect(model().attribute("activePage", "module"));
    }

    private MockHttpSession teacherSession() {
        MockHttpSession session = new MockHttpSession();
        User teacher = new User(1L, "Teacher User", "teacher@example.com", "hashed-password", "teacher", "ACTIVE");
        session.setAttribute("loggedInUser", teacher);
        session.setAttribute("userRole", "teacher");
        return session;
    }
}
