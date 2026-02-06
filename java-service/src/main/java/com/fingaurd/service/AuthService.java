package com.fingaurd.service;

import com.fingaurd.dto.request.LoginRequest;
import com.fingaurd.dto.response.AuthResponse;
import com.fingaurd.dto.response.UserResponse;
import com.fingaurd.exception.ValidationException;
import com.fingaurd.model.User;
import com.fingaurd.repository.UserRepository;
import com.fingaurd.security.JwtTokenProvider;
import com.fingaurd.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for authentication operations including refresh tokens and account lockout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    /**
     * Authenticate user, enforce lockout, return access + refresh tokens
     */
    public AuthResponse authenticateUser(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.getEmail());

        // Find user first (throws if not found)
        User user = userService.findByEmail(request.getEmail());

        // Check if account is locked
        if (user.getAccountLockedUntil() != null
                && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("Account is locked for user: {} until {}", user.getEmail(), user.getAccountLockedUntil());
            throw new BadCredentialsException(
                    "Account is locked due to too many failed login attempts. Try again after "
                            + user.getAccountLockedUntil());
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // Success: reset failed attempts
            userRepository.resetFailedLoginAttempts(user.getId());

            // Update last login
            userService.updateLastLogin(userPrincipal.getId());

            // Generate tokens
            String accessToken = tokenProvider.generateToken(userPrincipal.getId(), userPrincipal.getEmail());
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId(), userPrincipal.getEmail());

            // Re-read user to get updated lastLogin
            user = userService.findById(userPrincipal.getId());
            UserResponse userResponse = UserResponse.from(user);

            log.info("User authenticated successfully: {}", userPrincipal.getEmail());
            return AuthResponse.create(accessToken, refreshToken, jwtExpiration / 1000, userResponse);

        } catch (BadCredentialsException e) {
            // Record the failed attempt (runs in its own @Transactional, won't rollback)
            userRepository.incrementFailedLoginAttempts(user.getId());

            int newCount = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
                userRepository.lockAccount(user.getId(), lockedUntil);
                log.warn("Account locked for user {} after {} failed attempts until {}",
                        user.getEmail(), newCount, lockedUntil);
                throw new BadCredentialsException(
                        "Account locked due to too many failed attempts. Try again after " + lockedUntil);
            }

            log.warn("Authentication failed for email: {} (attempt {})", request.getEmail(), newCount);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Issue new access token from a valid refresh token
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new ValidationException("Invalid or expired refresh token");
        }
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new ValidationException("Token is not a refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
        String email = tokenProvider.getEmailFromToken(refreshToken);

        User user = userService.findById(userId);
        UserResponse userResponse = UserResponse.from(user);

        String newAccessToken = tokenProvider.generateToken(userId, email);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId, email);

        log.info("Access token refreshed for user: {}", email);
        return AuthResponse.create(newAccessToken, newRefreshToken, jwtExpiration / 1000, userResponse);
    }
}
