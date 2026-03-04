package com.example.bankcards.exception.exceptions;

public class TransferValidationException extends RuntimeException {
    public TransferValidationException(String message) {
        super(message);
    }
}