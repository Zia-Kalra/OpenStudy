package com.openstudy.openstudy.service.ai;

public class AiServiceException extends RuntimeException {

    private final AiErrorCode errorCode;
    private final Long retryAfterMs;

    public AiServiceException(AiErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public AiServiceException(AiErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }

    public AiServiceException(AiErrorCode errorCode, String message, Long retryAfterMs) {
        this(errorCode, message, null, retryAfterMs);
    }

    public AiServiceException(AiErrorCode errorCode, String message, Throwable cause, Long retryAfterMs) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryAfterMs = retryAfterMs;
    }

    public AiErrorCode getErrorCode() {
        return errorCode;
    }

    public Long getRetryAfterMs() {
        return retryAfterMs;
    }
}
