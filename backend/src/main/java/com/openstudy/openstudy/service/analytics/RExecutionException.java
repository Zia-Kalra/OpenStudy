package com.openstudy.openstudy.service.analytics;

/** Failure while running R. The message is safe to log; it never contains student data. */
public class RExecutionException extends Exception {

    public enum Kind { DISABLED, NOT_INSTALLED, SCRIPT_MISSING, TIMEOUT, SCRIPT_FAILED, BAD_OUTPUT, BUSY, IO }

    private final Kind kind;

    public RExecutionException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public RExecutionException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
