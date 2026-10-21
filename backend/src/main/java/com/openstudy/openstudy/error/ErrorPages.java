package com.openstudy.openstudy.error;

import com.openstudy.openstudy.service.ai.AiErrorCode;
import com.openstudy.openstudy.service.ai.AiServiceException;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.server.ResponseStatusException;

public final class ErrorPages {
    private ErrorPages() {
    }

    public static String newReferenceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static ErrorPageContent ofStatus(int status) {
        return switch (status) {
            case 400 -> new ErrorPageContent(400, "BAD_REQUEST", "That request could not be understood",
                    "Some of the information sent was missing or invalid. Check your input and try again.", false, false);
            case 401 -> new ErrorPageContent(401, "UNAUTHORIZED", "Sign in required",
                    "This page needs an authenticated teacher account. Teacher login is not available in this workspace yet.",
                    false, true);
            case 403 -> new ErrorPageContent(403, "FORBIDDEN", "You do not have access",
                    "You are signed in, but this area is limited to assigned teachers and classes.", false, false);
            case 404 -> new ErrorPageContent(404, "NOT_FOUND", "We could not find that page",
                    "The address may be mistyped, or the record is outside your assigned classes.", false, false);
            case 405 -> new ErrorPageContent(405, "METHOD_NOT_ALLOWED", "That action is not supported here",
                    "This page does not accept that kind of request. Return to the dashboard and use the available actions.",
                    false, false);
            case 408 -> new ErrorPageContent(408, "REQUEST_TIMEOUT", "The request took too long",
                    "The server stopped waiting for the request to finish. You can retry this page.", true, false);
            case 429 -> new ErrorPageContent(429, "TOO_MANY_REQUESTS", "Too many requests",
                    "Please wait a moment before trying again. If this continues, the upstream service may be rate limiting traffic.",
                    true, false);
            case 500 -> new ErrorPageContent(500, "INTERNAL_ERROR", "Something went wrong",
                    "OpenStudy hit an unexpected problem. Technical details were logged. You can retry or return to the dashboard.",
                    true, false);
            case 502 -> new ErrorPageContent(502, "BAD_GATEWAY", "An upstream service failed",
                    "A connected service did not return a usable response. Your last unsaved form input was not treated as stored.",
                    true, false);
            case 503 -> new ErrorPageContent(503, "SERVICE_UNAVAILABLE", "This service is temporarily unavailable",
                    "A required service is not ready. This can happen when authentication, the database, or an integration is offline.",
                    true, false);
            case 504 -> new ErrorPageContent(504, "GATEWAY_TIMEOUT", "An upstream service timed out",
                    "A connected service took too long to respond. You can retry. Nothing was saved unless you already saw a success message.",
                    true, false);
            default -> new ErrorPageContent(status >= 400 ? status : 500, "HTTP_ERROR",
                    "This request could not be completed",
                    "Please return to the dashboard or retry if this was a safe, read-only page.",
                    status >= 500, false);
        };
    }

    public static ErrorPageContent from(Throwable error, int fallbackStatus) {
        if (error instanceof AiServiceException ai) {
            return fromAi(ai);
        }
        if (error instanceof EmptyResultDataAccessException) {
            return ofStatus(404);
        }
        if (error instanceof DataIntegrityViolationException) {
            return new ErrorPageContent(400, "CONSTRAINT_VIOLATION", "That change could not be saved",
                    "The database rejected the change because it conflicts with existing records. Nothing was stored.",
                    false, false);
        }
        if (error instanceof QueryTimeoutException) {
            return ofStatus(504);
        }
        if (error instanceof CannotGetJdbcConnectionException) {
            return new ErrorPageContent(503, "DATABASE_UNAVAILABLE", "The database is temporarily unavailable",
                    "OpenStudy could not reach the database. Your unsaved input was not stored. You can retry shortly.",
                    true, false);
        }
        if (error instanceof DataAccessException) {
            return new ErrorPageContent(503, "DATABASE_ERROR", "The database is temporarily unavailable",
                    "A database operation failed. Technical details were logged. Nothing was saved unless you already saw confirmation.",
                    true, false);
        }
        if (error instanceof ResponseStatusException statusEx) {
            int status = statusEx.getStatusCode().value();
            ErrorPageContent base = ofStatus(status);
            String reason = statusEx.getReason();
            if (reason != null && !reason.isBlank() && isSafePublicReason(reason)) {
                return new ErrorPageContent(base.status(), base.code(), base.heading(), reason, base.retryable(),
                        base.suggestSignIn());
            }
            return base;
        }
        return ofStatus(fallbackStatus > 0 ? fallbackStatus : 500);
    }

    public static ErrorPageContent fromAi(AiServiceException error) {
        AiErrorCode code = error.getErrorCode();
        return switch (code) {
            case VALIDATION_FAILED -> new ErrorPageContent(400, code.name(), "The AI request was invalid",
                    "Check the form fields and try again. Your previous input is still on the page when the form is redisplayed.",
                    false, false);
            case MISSING_API_KEY, INVALID_API_KEY -> new ErrorPageContent(502, code.name(), "The AI service is not configured",
                    "Live Gemini access is not available with the current credentials. Dummy AI content was not generated.",
                    false, false);
            case INVALID_MODEL -> new ErrorPageContent(502, code.name(), "The selected AI model is unavailable",
                    "The configured Gemini model was rejected. Dummy AI content was not generated.", false, false);
            case RATE_LIMITED -> new ErrorPageContent(429, code.name(), "The AI service is rate limited",
                    "Gemini quota or rate limits were reached. Wait and retry. Dummy AI content was not generated.",
                    true, false);
            case TIMEOUT -> new ErrorPageContent(504, code.name(), "The AI service timed out",
                    "Gemini did not respond in time. You can retry. Dummy AI content was not generated.", true, false);
            case NETWORK_FAILURE -> new ErrorPageContent(502, code.name(), "The AI service could not be reached",
                    "OpenStudy could not complete the Gemini request. You can retry. Dummy AI content was not generated.",
                    true, false);
            case EMPTY_RESPONSE, INVALID_JSON, UNEXPECTED_RESPONSE -> new ErrorPageContent(502, code.name(),
                    "The AI service returned an unusable response",
                    "Gemini did not return a valid result. Dummy AI content was not generated. You can retry.",
                    true, false);
        };
    }

    static boolean isSafePublicReason(String reason) {
        String lower = reason.toLowerCase();
        return reason.length() <= 280
                && !lower.contains("exception")
                && !lower.contains("sql")
                && !lower.contains("jdbc")
                && !lower.contains("password")
                && !lower.contains("api key")
                && !lower.contains("apikey")
                && !lower.contains("secret")
                && !lower.contains("token")
                && !lower.contains("stack");
    }
}
