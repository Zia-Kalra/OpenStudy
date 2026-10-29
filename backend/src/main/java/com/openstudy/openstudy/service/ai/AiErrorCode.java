package com.openstudy.openstudy.service.ai;

public enum AiErrorCode {
    MISSING_API_KEY,
    INVALID_API_KEY,
    RATE_LIMITED,
    TIMEOUT,
    NETWORK_FAILURE,
    EMPTY_RESPONSE,
    INVALID_JSON,
    VALIDATION_FAILED,
    UNEXPECTED_RESPONSE,
    INVALID_MODEL
}
