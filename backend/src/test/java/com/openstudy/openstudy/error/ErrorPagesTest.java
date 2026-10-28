package com.openstudy.openstudy.error;

import static org.assertj.core.api.Assertions.assertThat;
import com.openstudy.openstudy.service.ai.AiErrorCode;
import com.openstudy.openstudy.service.ai.AiServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.server.ResponseStatusException;

class ErrorPagesTest {
    @Test
    void databaseFailuresStayGenericAndRetryable() {
        var db = ErrorPages.from(new CannotGetJdbcConnectionException("jdbc:mysql://secret-host/openstudy"), 500);
        assertThat(db.status()).isEqualTo(503);
        assertThat(db.code()).isEqualTo("DATABASE_UNAVAILABLE");
        assertThat(db.retryable()).isTrue();
        assertThat(db.explanation()).doesNotContain("jdbc", "secret-host", "mysql");

        var constraint = ErrorPages.from(new DataIntegrityViolationException("duplicate key"), 500);
        assertThat(constraint.status()).isEqualTo(400);
        assertThat(constraint.retryable()).isFalse();
    }

    @Test
    void groqFailuresDoNotInventDummyContent() {
        var missing = ErrorPages.fromAi(new AiServiceException(AiErrorCode.MISSING_API_KEY, "hidden"));
        assertThat(missing.status()).isEqualTo(502);
        assertThat(missing.explanation()).contains("GROQ_API_KEY", "No dummy AI content was generated")
                .doesNotContain("hidden");

        var rejected = ErrorPages.fromAi(new AiServiceException(AiErrorCode.INVALID_API_KEY, "private upstream detail"));
        assertThat(rejected.status()).isEqualTo(502);
        assertThat(rejected.explanation()).contains("Groq rejected", "Do not share the key")
                .doesNotContain("private upstream detail");

        var rate = ErrorPages.fromAi(new AiServiceException(AiErrorCode.RATE_LIMITED, "quota"));
        assertThat(rate.status()).isEqualTo(429);
        assertThat(rate.retryable()).isTrue();
    }

    @Test
    void unsafeStatusReasonsAreDropped() {
        var unsafe = ErrorPages.from(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "SQLException: password=secret api key leaked"), 400);
        assertThat(unsafe.explanation()).doesNotContain("SQLException", "password", "api key");
        var safe = ErrorPages.from(new ResponseStatusException(HttpStatus.FORBIDDEN, "This demo is restricted to the demo teacher."), 403);
        assertThat(safe.explanation()).isEqualTo("This demo is restricted to the demo teacher.");
    }
}
