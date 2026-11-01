package com.openstudy.openstudy.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.Duration;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConstructorFailureController())
            .setControllerAdvice(new GlobalExceptionHandler(mock(ErrorViewSupport.class)))
            .build();

    @Test
    void preservesWrappedRequestStatusWithoutExposingConstructorDetails() throws Exception {
        var result = mvc.perform(get("/api/test/constructor/request-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("password", "secret", "SQL", "Constructor", "Exception");
    }

    @Test
    void unknownConstructorFailuresRemainGenericServerErrors() throws Exception {
        var result = mvc.perform(get("/api/test/constructor/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("password", "secret", "Constructor", "IllegalArgumentException");
    }

    @Test
    void databaseDiagnosticsRetainCodesAndCauseTypesWithoutSecretMessages() {
        var sql = new SQLException("DB_PASSWORD=database-secret", "28000", 1045,
                new SSLHandshakeException("DB_TRUSTSTORE_PASSWORD=truststore-secret"));
        var failure = new CannotGetJdbcConnectionException("jdbc:mysql://secret-host", sql);

        assertThat(GlobalExceptionHandler.databaseFailureDetails(failure))
                .contains("CannotGetJdbcConnectionException", "SQLException[SQLState=28000, errorCode=1045]",
                        "SSLHandshakeException")
                .doesNotContain("database-secret", "truststore-secret", "secret-host", "DB_PASSWORD",
                        "DB_TRUSTSTORE_PASSWORD");
    }

    @Test
    void databaseDiagnosticsSanitizeUnsafeSqlState() {
        var failure = new SQLException("password=secret", "secret\nforged-log-entry", 0);

        assertThat(GlobalExceptionHandler.databaseFailureDetails(failure))
                .isEqualTo("SQLException[SQLState=unknown, errorCode=0]");
    }

    @Test
    void databaseDiagnosticsBoundCyclicCauseChains() {
        var first = new IllegalStateException("database-secret");
        var second = new SQLException("truststore-secret", "08001", 0);
        first.initCause(second);
        second.initCause(first);

        String details = assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> GlobalExceptionHandler.databaseFailureDetails(first));

        assertThat(details.split(" -> ")).hasSize(16);
        assertThat(details).doesNotContain("database-secret", "truststore-secret");
    }

    @Test
    void databaseFailuresKeepPublicResponseGeneric() throws Exception {
        var result = mvc.perform(get("/api/test/database"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "OpenStudy could not reach the database. Your unsaved input was not stored. You can retry shortly."))
                .andExpect(jsonPath("$.retryable").value(true))
                .andExpect(jsonPath("$.referenceId").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("database-secret", "SQLState", "SQLException", "1045", "28000", "causes");
    }

    @RestController
    static class ConstructorFailureController {
        @GetMapping("/api/test/database")
        void database() {
            throw new CannotGetJdbcConnectionException("password=database-secret",
                    new SQLException("password=database-secret", "28000", 1045));
        }

        @GetMapping("/api/test/constructor/request-status")
        void requestStatus() {
            throw new BeanInstantiationException(Object.class, "Constructor secret",
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL password=secret"));
        }

        @GetMapping("/api/test/constructor/unknown")
        void unknown() {
            throw new BeanInstantiationException(Object.class, "Constructor secret",
                    new IllegalArgumentException("password=secret"));
        }
    }
}
