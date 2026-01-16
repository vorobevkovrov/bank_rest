package com.example.bankcards.dto.request;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record RegisterRequest(@NotBlank(message = "Username is required")
                              @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
                              String username, @NotBlank(message = "Password is required")

                              @Size(min = 6, message = "Password must be at least 6 characters")
                              @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
                                      message = "Password must contain at least one digit," +
                                              " one lowercase and one uppercase letter")
                              String password,
                              Role role) {

    public void setRole(Role role) {
    }
}
