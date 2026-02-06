package com.fingaurd.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for User entity
 */
@DataJpaTest
@ActiveProfiles("test")
public class UserTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testUserCreation() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();

        // When
        User savedUser = entityManager.persistAndFlush(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashedpassword");
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getIsVerified()).isFalse();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getTransactions()).isEmpty();
    }

    @Test
    public void testUserWithDefaultValues() {
        // Given
        User user = User.builder()
                .username("defaultuser")
                .email("default@example.com")
                .passwordHash("password")
                .build();

        // When
        User savedUser = entityManager.persistAndFlush(user);

        // Then
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER); // Default value
        assertThat(savedUser.getIsActive()).isTrue(); // Default value
        assertThat(savedUser.getIsVerified()).isFalse(); // Default value
        assertThat(savedUser.getTransactions()).isEmpty(); // Default value
    }

    @Test
    public void testGetFullName() {
        // Given
        User user = User.builder()
                .username("fullnameuser")
                .email("fullname@example.com")
                .passwordHash("password")
                .firstName("John")
                .lastName("Doe")
                .build();

        // When
        User savedUser = entityManager.persistAndFlush(user);

        // Then
        assertThat(savedUser.getFullName()).isEqualTo("John Doe");
    }

    @Test
    public void testGetFullNameWithOnlyFirstName() {
        // Given
        User user = User.builder()
                .username("firstnameuser")
                .email("firstname@example.com")
                .passwordHash("password")
                .firstName("John")
                .build();

        // When
        User savedUser = entityManager.persistAndFlush(user);

        // Then
        assertThat(savedUser.getFullName()).isEqualTo("John");
    }

    @Test
    public void testGetFullNameWithOnlyLastName() {
        // Given
        User user = User.builder()
                .username("lastnameuser")
                .email("lastname@example.com")
                .passwordHash("password")
                .lastName("Doe")
                .build();

        // When
        User savedUser = entityManager.persistAndFlush(user);

        // Then
        assertThat(savedUser.getFullName()).isEqualTo("Doe");
    }

    @Test
    public void testGetFullNameWithNoNames() {
        // Given
        User user = User.builder()
                .username("nonameuser")
                .email("noname@example.com")
                .passwordHash("password")
                .build();

        // When
        User savedUser = entityManager.persistAndFlush(user);

        // Then
        assertThat(savedUser.getFullName()).isEqualTo("nonameuser");
    }

    @Test
    public void testIsAdmin() {
        // Given
        User adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash("password")
                .role(UserRole.ADMIN)
                .build();

        User regularUser = User.builder()
                .username("user")
                .email("user@example.com")
                .passwordHash("password")
                .role(UserRole.USER)
                .build();

        // When
        User savedAdmin = entityManager.persistAndFlush(adminUser);
        User savedUser = entityManager.persistAndFlush(regularUser);

        // Then
        assertThat(savedAdmin.isAdmin()).isTrue();
        assertThat(savedUser.isAdmin()).isFalse();
    }

    @Test
    public void testUserUpdate() {
        // Given
        User user = User.builder()
                .username("updateuser")
                .email("update@example.com")
                .passwordHash("password")
                .firstName("Original")
                .lastName("Name")
                .build();

        User savedUser = entityManager.persistAndFlush(user);
        LocalDateTime originalUpdatedAt = savedUser.getUpdatedAt();

        // When
        savedUser.setFirstName("Updated");
        savedUser.setLastName("Name");
        User updatedUser = entityManager.persistAndFlush(savedUser);

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    public void testUserWithLastLogin() {
        // Given
        User user = User.builder()
                .username("loginuser")
                .email("login@example.com")
                .passwordHash("password")
                .build();

        User savedUser = entityManager.persistAndFlush(user);

        // When
        LocalDateTime loginTime = LocalDateTime.now();
        savedUser.setLastLogin(loginTime);
        User updatedUser = entityManager.persistAndFlush(savedUser);

        // Then
        assertThat(updatedUser.getLastLogin()).isEqualTo(loginTime);
    }

    @Test
    public void testUserRoleEnum() {
        // Test all role values
        assertThat(UserRole.USER).isNotNull();
        assertThat(UserRole.ADMIN).isNotNull();
        assertThat(UserRole.MODERATOR).isNotNull();
        
        // Test role values
        assertThat(UserRole.USER.toString()).isEqualTo("USER");
        assertThat(UserRole.ADMIN.toString()).isEqualTo("ADMIN");
        assertThat(UserRole.MODERATOR.toString()).isEqualTo("MODERATOR");
    }

    @Test
    public void testUserWithTransactions() {
        // Given
        User user = User.builder()
                .username("transactionuser")
                .email("transaction@example.com")
                .passwordHash("password")
                .build();

        User savedUser = entityManager.persistAndFlush(user);

        Transaction transaction = Transaction.builder()
                .user(savedUser)
                .amount(java.math.BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .category("Food")
                .description("Test transaction")
                .transactionDate(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(transaction);
        entityManager.clear();

        // When
        User userWithTransactions = entityManager.find(User.class, savedUser.getId());

        // Then
        assertThat(userWithTransactions.getTransactions()).hasSize(1);
        assertThat(userWithTransactions.getTransactions().get(0).getAmount())
                .isEqualByComparingTo(java.math.BigDecimal.valueOf(100.00));
    }
}
