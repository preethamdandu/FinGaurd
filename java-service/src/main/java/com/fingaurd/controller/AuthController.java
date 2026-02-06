package com.fingaurd.controller;

import com.fingaurd.dto.request.LoginRequest;
import com.fingaurd.dto.request.UserRegistrationRequest;
import com.fingaurd.dto.response.AuthResponse;
import com.fingaurd.dto.response.UserResponse;
import com.fingaurd.service.AuthService;
import com.fingaurd.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Register a new user (signup)
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Signup request for email: {}", request.getEmail());
        UserResponse response = userService.registerUser(request);
        log.info("User registered successfully: {}", response.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user – returns access + refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.authenticateUser(request);
        log.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using a valid refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Token refresh request received");
        AuthResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user (stateless - just return success)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Logout request received");
        return ResponseEntity.noContent().build();
    }
}
