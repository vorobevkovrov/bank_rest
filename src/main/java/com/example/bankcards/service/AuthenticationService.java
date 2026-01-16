package com.example.bankcards.service;

import com.example.bankcards.dto.request.AuthenticationRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthenticationResponse;
import com.example.bankcards.entity.User;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    public User findByUsername(String username) {
        return new User();
    }

    public AuthenticationResponse register(RegisterRequest request) {
        return null;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        return null;
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        return null;
    }
}
