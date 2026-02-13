package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Role;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Builder
@ConfigurationProperties(prefix = "authenticationresponse")
public record AuthenticationResponse(String accessToken,
                                     String refreshToken,
                                     @DefaultValue("Bearer") String tokenType,
                                     Long expiresIn,
                                     String username,
                                     Role role,
                                     String message) {
}
