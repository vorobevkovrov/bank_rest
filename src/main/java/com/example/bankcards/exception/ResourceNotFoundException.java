package com.example.bankcards.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName) {
        super(String.format("%s ",
                resourceName));
    }
}
