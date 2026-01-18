package com.example.kafka.exception;

/**
 * Exception thrown when message validation fails.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
