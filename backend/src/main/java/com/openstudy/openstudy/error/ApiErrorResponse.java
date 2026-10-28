package com.openstudy.openstudy.error;

public record ApiErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        String referenceId,
        boolean retryable) {

    public static ApiErrorResponse of(ErrorPageContent content, String referenceId) {
        return new ApiErrorResponse(false, content.status(), content.code(), content.explanation(), referenceId,
                content.retryable());
    }
}
