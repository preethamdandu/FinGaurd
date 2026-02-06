package com.fingaurd.repository;

import com.fingaurd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Find active user by email
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    /**
     * Find active user by username
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isActive = true")
    Optional<User> findActiveUserByUsername(@Param("username") String username);
    
    /**
     * Check if email exists for different user (for updates)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id != :userId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("userId") UUID userId);
    
    /**
     * Check if username exists for different user (for updates)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.id != :userId")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("userId") UUID userId);
    
    /**
     * Count users by active status
     */
    long countByIsActive(Boolean isActive);
    
    /**
     * Count users by verified status
     */
    long countByIsVerified(Boolean isVerified);
    
    /**
     * Increment failed login attempts (runs in its own transaction so rollback doesn't undo it)
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") UUID userId);
    
    /**
     * Lock account until given time
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") UUID userId, @Param("lockedUntil") LocalDateTime lockedUntil);
    
    /**
     * Reset failed login attempts and unlock
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLockedUntil = null WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") UUID userId);
}
