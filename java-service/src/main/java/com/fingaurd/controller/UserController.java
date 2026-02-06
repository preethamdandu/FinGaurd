package com.fingaurd.controller;

import com.fingaurd.dto.request.UserUpdateRequest;
import com.fingaurd.dto.response.UserResponse;
import com.fingaurd.security.UserPrincipal;
import com.fingaurd.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user profile endpoints
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.debug("Getting profile for user: {}", currentUser.getEmail());
        
        UserResponse response = userService.getUserProfile(currentUser.getId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update current user's profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("Profile update requested for user: {}", currentUser.getEmail());
        
        UserResponse response = userService.updateUserProfile(currentUser.getId(), updateRequest);
        
        return ResponseEntity.ok(response);
    }
}
