package com.example.common.Exception;

import lombok.Getter;

/**
 * Custom runtime exception for handling application-specific errors.
 */
@Getter
public class AnyException extends RuntimeException {

    private final int statusCode;

    /**
     * Constructs a new AnyException with the given status code and message.
     *
     * @param statusCode the HTTP or custom error status code
     * @param message    the detail message describing the error
     */
    public AnyException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
