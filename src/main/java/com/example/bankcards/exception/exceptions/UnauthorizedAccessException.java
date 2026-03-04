package com.example.bankcards.exception.exceptions;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String resourceName) {
        super(String.format("%s",
                resourceName));
    }
}

