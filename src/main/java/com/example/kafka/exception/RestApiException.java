package com.example.kafka.exception;

/**
 * Exception thrown when REST API call fails after all retries.
 */
public class RestApiException extends Exception {

    private final Integer statusCode;

    public RestApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public RestApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public RestApiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RestApiException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
