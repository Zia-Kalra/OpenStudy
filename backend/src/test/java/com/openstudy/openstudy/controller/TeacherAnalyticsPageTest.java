package com.openstudy.openstudy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import com.openstudy.openstudy.provider.analytics.MockTeacherAnalyticsDataProvider;
import com.openstudy.openstudy.provider.analytics.TeacherAnalyticsDataProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.address=127.0.0.1")
@ActiveProfiles("mock")
class TeacherAnalyticsPageTest {
    @Value("${local.server.port}") private int port;
    @MockitoBean private TeacherAnalyticsDataProvider provider;

    @BeforeEach
    void mockData() {
        var realMock = new MockTeacherAnalyticsDataProvider();
        given(provider.load(any())).willAnswer(call -> realMock.load(call.getArgument(0)));
    }

    private HttpResponse<String> get(String route) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + route)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void compilesAllTeacherJspSectionsAndDeepLinks() throws Exception {
        String[][] routes = {{"/teacher/analytics", "Class Overview"}, {"/teacher/analytics/students", "Student Performance Explorer"},
                {"/teacher/analytics/students/S001", "Aarav Mehta"}, {"/teacher/analytics/assessments", "Question difficulty"},
                {"/teacher/analytics/reports", "Saved reports"}, {"/teacher/analytics/reports/R-REG", "Assessment progress model"}};
        for (var route : routes) {
            var response = get(route[0]);
            assertThat(response.statusCode()).as(route[0]).isEqualTo(200);
            assertThat(response.body()).contains(route[1], "DEMO WORKSPACE", "Teacher Analytics");
            assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        }
        assertThat(get("/css/teacher-analytics.css").statusCode()).isEqualTo(200);
    }

    @Test
    void selectedScopeAndSearchChangeTheActualTable() throws Exception {
        var response = get("/teacher/analytics/students?classId=CS-2A&search=Aarav");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Aarav Mehta", "1 of 12 students shown")
                .doesNotContain("Yash Jain", "Ananya Rao");
        var empty = get("/teacher/analytics/students?search=nonexistent");
        assertThat(empty.body()).contains("No students match these filters");
    }

    @Test
    void escapesNamesFeedbackAndSearchWithoutEmbeddingUserDataInScripts() throws Exception {
        String attack = "<img src=x onerror=alert(1)>";
        var actualMock = new MockTeacherAnalyticsDataProvider();
        org.mockito.BDDMockito.willAnswer(call -> {
            var original = actualMock.load((TeacherAnalyticsScope) call.getArgument(0));
            var students = new ArrayList<>(original.students());
            var first = students.getFirst();
            students.set(0, new Student(first.id(), first.rollNumber(), attack, first.classId(), first.subjectIds()));
            var results = original.results().stream().map(r -> new Result(r.studentId(), r.assessmentId(), r.status(), r.score(), attack)).toList();
            return new Snapshot(original.classes(), original.subjects(), students, original.assessments(), results, original.questions(), true);
        }).given(provider).load(any());
        var response = get("/teacher/analytics/students/S001");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(attack).contains("&lt;img src=x onerror=alert(1)&gt;");
        var searched = get("/teacher/analytics/students?search=%22%3E%3Cscript%3Emarker%3C%2Fscript%3E");
        assertThat(searched.statusCode()).isEqualTo(200);
        assertThat(searched.body()).doesNotContain("<script>marker</script>").contains("&lt;script&gt;marker&lt;/script&gt;");
    }

    @Test
    void existingDashboardAndMockDatabaseIsolationStillWork() throws Exception {
        assertThat(get("/teacher/dashboard").statusCode()).isEqualTo(200);
        assertThat(get("/api/db-test").statusCode()).isEqualTo(404);
        assertThat(get("/teacher/analytics/students/S999").statusCode()).isEqualTo(404);
    }
}
