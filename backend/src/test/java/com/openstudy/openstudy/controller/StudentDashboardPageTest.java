package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.MockJdbcTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1"
})
@ActiveProfiles("mock")

@Import(MockJdbcTestConfiguration.class)

class StudentDashboardPageTest {
    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

        private String loginForSessionCookie() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> login = client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=student@example.com&password=student123&role=student"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
        return login.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        }

        private HttpResponse<String> authenticatedGet(String path, String sessionCookie) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Cookie", sessionCookie)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
        }

    @Test
    void studentPagesAndAssetsAreAvailable() throws Exception {
        var loginPage = get("/login");
        assertThat(loginPage.statusCode()).isEqualTo(200);
        assertThat(loginPage.body()).contains("/forgot-password", "/signup").doesNotContain("Contact your admin", "student123");
        var signupPage = get("/signup");
        assertThat(signupPage.statusCode()).isEqualTo(200);
        assertThat(signupPage.body()).contains("Create Student Account", "name=\"fullName\"", "name=\"confirmPassword\"");
        var forgotPasswordPage = get("/forgot-password");
        assertThat(forgotPasswordPage.statusCode()).isEqualTo(200);
        assertThat(forgotPasswordPage.body()).contains("Send Reset Link");
        var invalidResetPage = get("/reset-password?token=invalid");
        assertThat(invalidResetPage.statusCode()).isEqualTo(200);
        assertThat(invalidResetPage.body()).contains("This reset link is invalid or has expired");

        String sessionCookie = loginForSessionCookie();
        assertThat(get("/student/login").statusCode()).isEqualTo(200);
        var dashboard = authenticatedGet("/student/dashboard", sessionCookie);
        assertThat(dashboard.statusCode()).isEqualTo(200);
        assertThat(dashboard.body()).contains("Welcome Back, Student User!");

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> update = client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/student/profile"))
                .header("Cookie", sessionCookie)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "studentId=MY-COLLEGE-ID-7&fullName=Updated+Student&email=updated%40example.com"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(update.statusCode()).isEqualTo(302);

        var profile = authenticatedGet("/student/profile", sessionCookie);
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.body()).contains(
            "MY-COLLEGE-ID-7", "Updated Student", "updated@example.com", "Account ID (system-generated)",
            "id=\"profileEditForm\"", "Save Changes");
        assertThat(profile.body()).doesNotContain("23CS101", "ABC Engineering College");
        var availableQuizzes = authenticatedGet("/student/quizzes", sessionCookie);
        assertThat(availableQuizzes.statusCode()).isEqualTo(200);
        assertThat(availableQuizzes.body()).contains(
                "My Quizzes", "id=\"pendingQuizzesTab\"", "id=\"attemptedQuizzesTab\"",
                "id=\"quizStartDialog\"", "id=\"quizList\"", "student-quiz.js")
                .doesNotContain("Java Basics Quiz", "DBMS Concepts Quiz");
        assertThat(authenticatedGet("/student/assignments", sessionCookie).statusCode()).isEqualTo(200);
        assertThat(authenticatedGet("/student/results", sessionCookie).statusCode()).isEqualTo(200);
        assertThat(authenticatedGet("/student/performance", sessionCookie).statusCode()).isEqualTo(200);
        assertThat(get("/css/student-dashboard.css").statusCode()).isEqualTo(200);
        assertThat(get("/js/student-dashboard.js").statusCode()).isEqualTo(200);
    }
}
