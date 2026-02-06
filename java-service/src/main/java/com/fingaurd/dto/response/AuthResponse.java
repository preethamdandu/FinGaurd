package com.fingaurd.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for authentication
 */
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
    private LocalDateTime expiresAt;
    
    /**
     * Create auth response with user info
     */
    public static AuthResponse create(String token, long expiresIn, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(expiresIn))
                .build();
    }
}
