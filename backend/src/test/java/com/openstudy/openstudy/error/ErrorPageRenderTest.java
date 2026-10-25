package com.openstudy.openstudy.error;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.CookieManager;
import java.net.CookiePolicy;
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
        return get(HttpClient.newHttpClient(), path, accept);
    }

    private HttpResponse<String> get(HttpClient client, String path, String accept) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Accept", accept).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient loggedInTeacher() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var login = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("email=teacher@example.com&password=teacher123&role=teacher"))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(URI.create(login.headers().firstValue("location").orElseThrow()).getPath().split(";", 2)[0])
                .isEqualTo("/teacher/dashboard");
        return client;
    }

    @Test
    void teacherAnalyticsNotFoundUsesSafeErrorPage() throws Exception {
        var response = get(loggedInTeacher(), "/teacher/analytics/students/S999", "text/html");
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
