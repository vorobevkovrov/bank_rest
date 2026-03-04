package com.example.bankcards.exception.exceptions;

public class CardRequestStatusException extends RuntimeException{
    public CardRequestStatusException(String message) {
        super(message);
    }
}
