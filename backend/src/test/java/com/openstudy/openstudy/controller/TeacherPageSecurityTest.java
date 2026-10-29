package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.MockJdbcTestConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import com.openstudy.openstudy.dto.DashboardStatsDTO;
import com.openstudy.openstudy.dto.RecentActivityDTO;
import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.UpcomingDeadlineDTO;
import com.openstudy.openstudy.provider.TeacherDashboardDataProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Uses embedded Tomcat: MockMvc alone does not compile or render JSPs. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1",
        "openstudy.ai.provider=mock",
        "openstudy.modules.quizzes-path=/teacher/coming-soon?module=quizzes&marker=\"attribute-marker\""
})
@ActiveProfiles("mock")

@Import(MockJdbcTestConfiguration.class)

class TeacherPageSecurityTest {
    @Value("${local.server.port}")
    private int port;
    @MockitoBean
    private TeacherDashboardDataProvider provider;
    private static final String ATTACK = "<script>window.reviewMarker=1</script>";

    @BeforeEach
    void supplyUntrustedDashboardData() {
        given(provider.getTeacherIdentity()).willReturn(new TeacherIdentityDTO("demo", ATTACK, "demo@example.invalid", ATTACK, true));
        given(provider.getStatistics()).willReturn(new DashboardStatsDTO(1, 1, 1, 1));
        given(provider.getRecentActivity()).willReturn(List.of(new RecentActivityDTO(
                "1", ATTACK, ATTACK, ATTACK, LocalDateTime.now(), ATTACK)));
        given(provider.getUpcomingDeadlines()).willReturn(List.of(new UpcomingDeadlineDTO(
                "1", ATTACK, ATTACK, LocalDateTime.now(), ATTACK, ATTACK)));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> authenticatedGet(String path) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest loginRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=teacher@example.com&password=teacher123&role=teacher"))
                .build();
        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode()).isEqualTo(302);
        String sessionCookie = loginResponse.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];

        HttpRequest pageRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Cookie", sessionCookie)
                .GET()
                .build();
        return client.send(pageRequest, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void actualJspEscapesDashboardDataAndNavigationAttributes() throws Exception {
        var response = authenticatedGet("/teacher/dashboard");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(ATTACK).contains("&lt;script&gt;")
                .contains("&amp;marker=&#034;attribute-marker&#034;")
                .contains("aria-controls=\"osNavLinks\"", "aria-expanded=\"false\"", "data-os-nav-toggle");
    }

    @Test
    void actualPlaceholderDoesNotReflectQueryPayload() throws Exception {
        var response = authenticatedGet("/teacher/coming-soon?module=%3Cscript%3Ewindow.queryMarker%3D1%3C%2Fscript%3E");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain("window.queryMarker=1", ATTACK).contains("Module key: module");
    }

    @Test
    void mockProfileDisablesDatabaseDiagnosticAndServesAssets() throws Exception {
        assertThat(get("/api/db-test").statusCode()).isEqualTo(404);
        assertThat(get("/css/teacher-dashboard.css").statusCode()).isEqualTo(200);
        assertThat(get("/js/teacher-dashboard.js").statusCode()).isEqualTo(200);
    }
}
