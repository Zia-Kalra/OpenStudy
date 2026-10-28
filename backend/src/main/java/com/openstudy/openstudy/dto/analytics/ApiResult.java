package com.openstudy.openstudy.dto.analytics;

import java.time.Instant;
import java.util.List;

/** Consistent envelope for every analytics endpoint. */
public record ApiResult<T>(boolean success, T data, String message, List<String> warnings, Instant generatedAt) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null, List.of(), Instant.now());
    }

    public static <T> ApiResult<T> ok(T data, List<String> warnings) {
        return new ApiResult<>(true, data, null, warnings == null ? List.of() : warnings, Instant.now());
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(false, null, message, List.of(), Instant.now());
    }
}
