package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "authenticationresponse")
public record AuthenticationResponse(String accessToken,
                                     String refreshToken,
                                     @DefaultValue("Bearer") String tokenType,
                                     Long expiresIn,
                                     String username,
                                     Role role,
                                     String message) {
}
