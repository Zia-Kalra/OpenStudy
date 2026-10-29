package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.dto.analytics.ApiResult;
import com.openstudy.openstudy.service.analytics.AnalyticsAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps analytics failures to HTTP codes without leaking SQL, stack traces or student data. */
@RestControllerAdvice(assignableTypes = AnalyticsApiController.class)
// Must run before the global handler, which would turn access errors into generic 500 pages.
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile("!mock")
public class AnalyticsExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsExceptionHandler.class);

    @ExceptionHandler(AnalyticsAccessException.class)
    ResponseEntity<ApiResult<Void>> access(AnalyticsAccessException e) {
        return ResponseEntity.status(e.status()).body(ApiResult.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResult<Void>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.error(e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResult<Void>> database(DataAccessException e) {
        log.error("Analytics database error: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResult.error("Analytics data is temporarily unavailable. Please try again later."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResult<Void>> unexpected(Exception e) {
        log.error("Unexpected analytics error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("Something went wrong while preparing analytics."));
    }
}
