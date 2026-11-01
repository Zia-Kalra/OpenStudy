package com.openstudy.openstudy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.StudentQuizService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StudentQuizApiControllerTest {

    @Mock
    private StudentQuizService quizzes;

    @InjectMocks
    private StudentQuizApiController controller;

    @Test
    void derivesStudentIdentityFromAuthenticatedSession() {
        when(quizzes.availableQuizzes(42L)).thenReturn(List.of());
        MockHttpSession session = studentSession(42L, "STUDENT");

        controller.available(session, studentQuizRequest());

        verify(quizzes).availableQuizzes(42L);
    }

    @Test
    void rejectsUnauthenticatedAndTeacherSessionsFromStudentApi() {
        ResponseStatusException unauthenticated = assertThrows(ResponseStatusException.class,
                () -> controller.available((HttpSession) null, studentQuizRequest()));
        ResponseStatusException teacher = assertThrows(ResponseStatusException.class,
                () -> controller.available(studentSession(9L, "TEACHER"), studentQuizRequest()));

        assertEquals(HttpStatus.UNAUTHORIZED, unauthenticated.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, teacher.getStatusCode());
    }

    @Test
    void listingEndpointSerializesQuizFromUnenrolledSubjectAsExpectedByPage() throws Exception {
        when(quizzes.availableQuizzes(42L)).thenReturn(List.of(Map.of(
                "quiz_id", 5L,
                "title", "Java Basics",
                "instructions", "Choose one answer.",
                "subject_name", "Java",
                "time_limit_minutes", 20,
                "question_count", 8,
                "total_marks", 8,
                "available_from", "2026-10-01T09:00:00",
                "available_until", "2026-10-30T17:00:00",
                "supported_for_attempt", true)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/student/quizzes").session(studentSession(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.is(1)))
                .andExpect(jsonPath("$[0].quiz_id").value(5))
                .andExpect(jsonPath("$[0].title").value("Java Basics"))
                .andExpect(jsonPath("$[0].subject_name").value("Java"))
                .andExpect(jsonPath("$[0].time_limit_minutes").value(20))
                .andExpect(jsonPath("$[0].question_count").value(8));
    }

    @Test
    void listingEndpointReturnsPublishedQuizJsonForAuthenticatedStudent() throws Exception {
        when(quizzes.availableQuizzes(42L)).thenReturn(List.of(Map.of(
                "quiz_id", 5L,
                "title", "Java Basics",
                "subject_name", "Java",
                "instructions", "Choose the best answer.",
                "time_limit_minutes", 20,
                "question_count", 8,
                "total_marks", 8,
                "available_from", "2026-10-01T09:00:00",
                "available_until", "2026-10-30T17:00:00",
                "supported_for_attempt", true)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/student/quizzes").session(studentSession(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quiz_id").value(5))
                .andExpect(jsonPath("$[0].title").value("Java Basics"))
                .andExpect(jsonPath("$[0].subject_name").value("Java"))
                .andExpect(jsonPath("$[0].time_limit_minutes").value(20))
                .andExpect(jsonPath("$[0].question_count").value(8));
    }

    private MockHttpSession studentSession(long id, String role) {
        User user = new User();
        user.setUserId(id);
        user.setRole(role);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", user);
        return session;
    }

    private MockHttpServletRequest studentQuizRequest() {
        return new MockHttpServletRequest("GET", "/api/student/quizzes");
    }
}
