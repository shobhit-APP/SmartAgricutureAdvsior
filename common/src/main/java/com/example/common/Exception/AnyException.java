package com.example.common.Exception;

import lombok.Getter;

@Getter
public class AnyException extends RuntimeException {
    private final int statusCode;
    public AnyException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

}