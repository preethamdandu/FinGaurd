package com.fingaurd.service;

import com.fingaurd.dto.request.LoginRequest;
import com.fingaurd.dto.response.AuthResponse;
import com.fingaurd.dto.response.UserResponse;
import com.fingaurd.model.User;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    /**
     * Authenticate user and return JWT token
     */
    @Transactional
    public AuthResponse authenticateUser(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.getEmail());
        
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            // Update last login
            userService.updateLastLogin(userPrincipal.getId());
            
            // Generate JWT token
            String token = tokenProvider.generateToken(userPrincipal.getId(), userPrincipal.getEmail());
            
            // Get user details
            User user = userService.findById(userPrincipal.getId());
            UserResponse userResponse = UserResponse.from(user);
            
            log.info("User authenticated successfully: {}", userPrincipal.getEmail());
            
            return AuthResponse.create(token, jwtExpiration / 1000, userResponse);
            
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        } catch (Exception e) {
            log.error("Authentication error for email: {}", request.getEmail(), e);
            throw new BadCredentialsException("Authentication failed");
        }
    }
}
