package com.smis.security;

import java.util.UUID;
import org.slf4j.LoggerFactory;

/** Exception messages can contain SQL parameters or tokens; log types and frames only. */
public final class ErrorReferences {
    private ErrorReferences() {}
    public static String record(Throwable error) {
        String reference = UUID.randomUUID().toString();
        StringBuilder detail = new StringBuilder();
        for (int cause = 0; error != null && cause < 8; cause++, error = error.getCause()) {
            detail.append('\n').append(error.getClass().getName());
            for (StackTraceElement frame : error.getStackTrace()) detail.append("\n\tat ").append(frame);
        }
        LoggerFactory.getLogger(ErrorReferences.class).error("Application error reference={}{}", reference, detail);
        return reference;
    }
}
