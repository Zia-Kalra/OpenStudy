package com.openstudy.openstudy.service.ai;

public class AiServiceException extends RuntimeException {

    private final AiErrorCode errorCode;

    public AiServiceException(AiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiServiceException(AiErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AiErrorCode getErrorCode() {
        return errorCode;
    }
}
