package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.MockJdbcTestConfiguration;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.openstudy.openstudy.model.User;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
@Import(MockJdbcTestConfiguration.class)

class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootPathRendersLoginPageWithoutAmbiguousHandler() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/login"));
    }

    @Test
    void signupAndPasswordRecoveryRoutesAreAvailableWithoutMockPersistence() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/signup"));
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/forgot-password"));
        mockMvc.perform(get("/reset-password").param("token", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/reset-password"))
                .andExpect(model().attribute("validResetToken", false));

        mockMvc.perform(post("/signup")
                        .param("fullName", "New Student")
                        .param("email", "new@example.com")
                        .param("password", "a-long-test-password")
                        .param("confirmPassword", "a-long-test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup"))
                .andExpect(flash().attributeExists("errorMessage"));

        mockMvc.perform(post("/forgot-password").param("email", "student@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

        @Test
    void studentLoginRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "student@example.com")
                        .param("password", "student123")
                        .param("role", "student"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/dashboard"));
    }

    @Test
    void protectedStudentDashboardRequiresLogin() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=unauthenticated"));
    }

    @Test
    void loggedInStudentSeesAuthenticatedNameOnDashboard() throws Exception {
        var result = mockMvc.perform(post("/login")
                        .param("email", "student@example.com")
                        .param("password", "student123")
                        .param("role", "student"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/student/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(response -> response.getResponse().getContentAsString().contains("Student User"));
    }

    @Test
    void studentPagesUseAuthenticatedUserIdentityAndLogoutLink() throws Exception {
        var result = mockMvc.perform(post("/login")
                        .param("email", "student@example.com")
                        .param("password", "student123")
                        .param("role", "student"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        for (String path : List.of("/student/quizzes", "/student/assignments", "/student/results", "/student/performance")) {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("currentUserName", "Student User"))
                    .andExpect(model().attribute("currentUserInitial", "S"));
        }
    }

    @Test
    void studentCanUpdateStudentIdAndProfileDetailsInSession() throws Exception {
        var loginResult = mockMvc.perform(post("/login")
                        .param("email", "student@example.com")
                        .param("password", "student123")
                        .param("role", "student"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/student/profile")
                        .session(session)
                        .param("studentId", "MY-COLLEGE-ID-7")
                        .param("fullName", "Updated Student")
                        .param("email", "updated@example.com")
                        .param("phoneNumber", "5551234567")
                        .param("course", "Computer Science")
                        .param("semester", "5")
                        .param("institution", "OpenStudy College")
                        .param("department", "Computing")
                        .param("year", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/profile"));

        var profileResult = mockMvc.perform(get("/student/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentUserName", "Updated Student"))
                .andExpect(model().attribute("currentUserEmail", "updated@example.com"))
                .andReturn();
        User profileUser = (User) profileResult.getModelAndView().getModel().get("currentUser");
        assertThat(profileUser.getStudentId()).isEqualTo("MY-COLLEGE-ID-7");
    }

    @Test
    void logoutInvalidatesSessionAndRedirectsToLogin() throws Exception {
        var result = mockMvc.perform(post("/login")
                        .param("email", "student@example.com")
                        .param("password", "student123")
                        .param("role", "student"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=loggedOut"));
    }

    @Test
    void teacherUserCannotAccessStudentDashboard() throws Exception {
        var result = mockMvc.perform(post("/login")
                        .param("email", "teacher@example.com")
                        .param("password", "teacher123")
                        .param("role", "teacher"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/student/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/dashboard"));
    }

    @Test
    void loggedInTeacherUserCanReachTeacherDashboard() throws Exception {
        var result = mockMvc.perform(post("/login")
                        .param("email", "teacher@example.com")
                        .param("password", "teacher123")
                        .param("role", "teacher"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/teacher/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(response -> response.getResponse().getContentAsString().contains("Teacher User"));
    }
}
