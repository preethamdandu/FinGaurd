package com.fingaurd.service;

import com.fingaurd.dto.request.UserRegistrationRequest;
import com.fingaurd.dto.request.UserUpdateRequest;
import com.fingaurd.dto.response.UserResponse;
import com.fingaurd.exception.UserAlreadyExistsException;
import com.fingaurd.exception.UserNotFoundException;
import com.fingaurd.exception.ValidationException;
import com.fingaurd.model.User;
import com.fingaurd.model.UserRole;
import com.fingaurd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Comprehensive service for user-related business operations
 * Handles user registration, authentication, profile management, and security
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Business rules and constants
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCKOUT_MINUTES = 30;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,50}$"
    );
    
    /**
     * Register a new user with comprehensive validation and security
     */
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Starting user registration for email: {}", request.getEmail());
        
        // Comprehensive validation
        validateRegistrationRequest(request);
        
        // Check for existing users
        validateUserUniqueness(request.getEmail(), request.getUsername());
        
        // Create user entity with security best practices
        User user = createUserFromRequest(request);
        
        // Save user
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully with ID: {} and email: {}", 
                savedUser.getId(), savedUser.getEmail());
        
        return UserResponse.from(savedUser);
    }
    
    /**
     * Find user by email with security checks
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email cannot be null or empty");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email format");
        }
        
        return userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
    
    /**
     * Find user by ID with security checks
     */
    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        return userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
    
    /**
     * Find user by username with security checks
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ValidationException("Username cannot be null or empty");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException("Invalid username format");
        }
        
        return userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }
    
    /**
     * Get user profile with comprehensive information
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = findById(userId);
        
        // Log profile access for audit
        log.debug("Profile accessed for user: {}", user.getEmail());
        
        return UserResponse.from(user);
    }
    
    /**
     * Update user profile with validation and security checks
     */
    @Transactional
    public UserResponse updateUserProfile(UUID userId, UserUpdateRequest request) {
        log.info("Updating profile for user: {}", userId);
        
        User user = findById(userId);
        
        // Validate update request
        validateUpdateRequest(request);
        
        // Check email uniqueness if email is being changed
        if (StringUtils.hasText(request.getEmail()) && 
            !request.getEmail().equals(user.getEmail())) {
            validateEmailUniqueness(request.getEmail(), userId);
        }
        
        // Check username uniqueness if username is being changed
        if (StringUtils.hasText(request.getUsername()) && 
            !request.getUsername().equals(user.getUsername())) {
            validateUsernameUniqueness(request.getUsername(), userId);
        }
        
        // Update user fields
        updateUserFields(user, request);
        
        // Save updated user
        User updatedUser = userRepository.save(user);
        
        log.info("Profile updated successfully for user: {}", updatedUser.getEmail());
        
        return UserResponse.from(updatedUser);
    }
    
    /**
     * Update user password with security validation
     */
    @Transactional
    public void updatePassword(UUID userId, String currentPassword, String newPassword) {
        log.info("Updating password for user: {}", userId);
        
        User user = findById(userId);
        
        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        
        // Validate new password
        validatePassword(newPassword);
        
        // Check if new password is different from current
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ValidationException("New password must be different from current password");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password updated successfully for user: {}", user.getEmail());
    }
    
    /**
     * Deactivate user account (soft delete)
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("Deactivating user account: {}", userId);
        
        User user = findById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User account deactivated: {}", user.getEmail());
    }
    
    /**
     * Reactivate user account
     */
    @Transactional
    public void reactivateUser(UUID userId) {
        log.info("Reactivating user account: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("User account reactivated: {}", user.getEmail());
    }
    
    /**
     * Update last login timestamp and handle login tracking
     */
    @Transactional
    public void updateLastLogin(UUID userId) {
        User user = findById(userId);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        log.debug("Updated last login for user: {}", user.getEmail());
    }
    
    /**
     * Verify user email (mark as verified)
     */
    @Transactional
    public void verifyUserEmail(UUID userId) {
        log.info("Verifying email for user: {}", userId);
        
        User user = findById(userId);
        user.setIsVerified(true);
        userRepository.save(user);
        
        log.info("Email verified for user: {}", user.getEmail());
    }
    
    /**
     * Get all users with pagination (admin only)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Retrieving all users with pagination");
        
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }
    
    /**
     * Search users by criteria (admin only)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String email, String username, Boolean isActive, Pageable pageable) {
        log.debug("Searching users with criteria - email: {}, username: {}, isActive: {}", 
                email, username, isActive);
        
        // This would require custom repository methods for complex search
        // For now, we'll use basic filtering
        if (StringUtils.hasText(email)) {
            Page<User> allUsers = userRepository.findAll(pageable);
            List<UserResponse> filteredUsers = allUsers.getContent().stream()
                    .filter(user -> user.getEmail().contains(email))
                    .map(UserResponse::from)
                    .collect(Collectors.toList());
            return new PageImpl<>(filteredUsers, pageable, filteredUsers.size());
        }
        
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }
    
    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Get user statistics (admin only)
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long verifiedUsers = userRepository.countByIsVerified(true);
        
        return UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(totalUsers - activeUsers)
                .verifiedUsers(verifiedUsers)
                .unverifiedUsers(totalUsers - verifiedUsers)
                .build();
    }
    
    // Private helper methods
    
    private void validateRegistrationRequest(UserRegistrationRequest request) {
        if (request == null) {
            throw new ValidationException("Registration request cannot be null");
        }
        
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ValidationException("Email is required");
        }
        
        if (!StringUtils.hasText(request.getUsername())) {
            throw new ValidationException("Username is required");
        }
        
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password is required");
        }
        
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new ValidationException("Invalid email format");
        }
        
        if (!USERNAME_PATTERN.matcher(request.getUsername()).matches()) {
            throw new ValidationException("Username must be 3-50 characters and contain only letters, numbers, and underscores");
        }
        
        validatePassword(request.getPassword());
    }
    
    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ValidationException("Password is required");
        }
        
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }
        
        if (password.length() > 128) {
            throw new ValidationException("Password must not exceed 128 characters");
        }
        
        // Check for common weak passwords
        if (isCommonPassword(password)) {
            throw new ValidationException("Password is too common and not secure");
        }
    }
    
    private boolean isCommonPassword(String password) {
        List<String> commonPasswords = List.of(
            "password", "123456", "123456789", "qwerty", "abc123", 
            "password123", "admin", "letmein", "welcome", "monkey"
        );
        
        return commonPasswords.contains(password.toLowerCase());
    }
    
    private void validateUserUniqueness(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already registered: " + email);
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already taken: " + username);
        }
    }
    
    private void validateEmailUniqueness(String email, UUID excludeUserId) {
        if (userRepository.existsByEmailAndIdNot(email, excludeUserId)) {
            throw new UserAlreadyExistsException("Email already registered: " + email);
        }
    }
    
    private void validateUsernameUniqueness(String username, UUID excludeUserId) {
        if (userRepository.existsByUsernameAndIdNot(username, excludeUserId)) {
            throw new UserAlreadyExistsException("Username already taken: " + username);
        }
    }
    
    private void validateUpdateRequest(UserUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        if (StringUtils.hasText(request.getEmail()) && 
            !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new ValidationException("Invalid email format");
        }
        
        if (StringUtils.hasText(request.getUsername()) && 
            !USERNAME_PATTERN.matcher(request.getUsername()).matches()) {
            throw new ValidationException("Username must be 3-50 characters and contain only letters, numbers, and underscores");
        }
    }
    
    private User createUserFromRequest(UserRegistrationRequest request) {
        return User.builder()
                .username(request.getUsername().toLowerCase().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(StringUtils.hasText(request.getFirstName()) ? 
                    request.getFirstName().trim() : null)
                .lastName(StringUtils.hasText(request.getLastName()) ? 
                    request.getLastName().trim() : null)
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();
    }
    
    private void updateUserFields(User user, UserUpdateRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().toLowerCase().trim());
        }
        
        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(request.getUsername().toLowerCase().trim());
        }
        
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }
        
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName().trim());
        }
    }
    
    /**
     * Inner class for user statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private long verifiedUsers;
        private long unverifiedUsers;
    }
}