package com.openstudy.openstudy.service.analytics;

import org.springframework.http.HttpStatus;

/** Thrown when a caller is not signed in (401), has the wrong role or scope (403), or sends a bad request (400). */
public class AnalyticsAccessException extends RuntimeException {

    private final HttpStatus status;

    public AnalyticsAccessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
