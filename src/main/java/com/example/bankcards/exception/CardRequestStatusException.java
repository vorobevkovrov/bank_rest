package com.example.bankcards.exception;

public class CardRequestStatusException extends RuntimeException{
    public CardRequestStatusException(String message) {
        super(message);
    }
}
