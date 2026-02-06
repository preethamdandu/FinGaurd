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
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
    private LocalDateTime expiresAt;
    
    /**
     * Create auth response with access + refresh tokens
     */
    public static AuthResponse create(String accessToken, String refreshToken,
                                       long expiresInSeconds, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds))
                .build();
    }
    
    /** Backward-compatible factory (no refresh token) */
    public static AuthResponse create(String token, long expiresInSeconds, UserResponse user) {
        return create(token, null, expiresInSeconds, user);
    }
}
