package com.openstudy.openstudy.error;

/** Safe, user-facing copy for an HTTP error. Never includes stack traces or secrets. */
public record ErrorPageContent(
        int status,
        String code,
        String heading,
        String explanation,
        boolean retryable,
        boolean suggestSignIn) {
}
