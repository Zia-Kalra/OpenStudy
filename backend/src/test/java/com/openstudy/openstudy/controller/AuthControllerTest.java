package com.openstudy.openstudy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AuthControllerTest {

    @Test
    void successfulSignupReturnsGeneratedAccountIdAndStartsSession() {
        AuthService authService = mock(AuthService.class);
        User createdUser = new User(48127L, "New Student", "new@example.com", "bcrypt-hash", "student", "ACTIVE");
        when(authService.registerStudent("New Student", "new@example.com", "a-long-test-password"))
                .thenReturn(createdUser);
        AuthController controller = new AuthController(authService);
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String result = controller.signup("New Student", "new@example.com", "a-long-test-password",
                "a-long-test-password", session, redirect);

        assertThat(result).isEqualTo("redirect:/student/dashboard");
        assertThat(session.getAttribute("loggedInUser")).isSameAs(createdUser);
        assertThat(session.getAttribute("userRole")).isEqualTo("student");
        assertThat(redirect.getFlashAttributes().get("newAccountId")).isEqualTo(48127L);
    }
}
