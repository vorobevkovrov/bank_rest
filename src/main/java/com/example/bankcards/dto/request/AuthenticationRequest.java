package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AuthenticationRequest(@NotBlank(message = "Username is required")
                                    String username,
                                    @NotBlank(message = "Password is required")
                                    String password) {

}

