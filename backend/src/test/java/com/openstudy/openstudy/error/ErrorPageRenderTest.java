package com.openstudy.openstudy.error;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.address=127.0.0.1")
@ActiveProfiles("mock")
class ErrorPageRenderTest {
    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> get(String path, String accept) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Accept", accept).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void teacherAnalyticsNotFoundUsesSafeErrorPage() throws Exception {
        var response = get("/teacher/analytics/students/S999", "text/html");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        assertThat(response.body()).contains("We could not find that page", "Reference")
                .doesNotContain("Exception", "sql", "stack", "S999");
    }

    @Test
    void apiErrorsReturnJsonWithoutWhitelabelHtml() throws Exception {
        var response = get("/api/does-not-exist", "application/json");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("application/json");
        assertThat(response.body()).contains("\"success\":false", "NOT_FOUND").doesNotContain("<html", "Whitelabel");
    }
}
