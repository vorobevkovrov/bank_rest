package com.example.bankcards.exception.exceptions;

public class SameCardTransferException extends RuntimeException {
    public SameCardTransferException(String message) {
        super(message);
    }
}
