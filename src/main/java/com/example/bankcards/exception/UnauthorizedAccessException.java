package com.example.bankcards.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String resourceName) {
        super(String.format("%s не найден '",
                resourceName));
    }
}

