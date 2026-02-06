package com.fingaurd.repository;

import com.fingaurd.model.User;
import com.fingaurd.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for UserRepository
 */
@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User adminUser;
    private User inactiveUser;

    @BeforeEach
    public void setUp() {
        // Create test users
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();

        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash("admin123")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .isVerified(true)
                .build();

        inactiveUser = User.builder()
                .username("inactive")
                .email("inactive@example.com")
                .passwordHash("password")
                .firstName("Inactive")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(false)
                .isVerified(false)
                .build();

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(adminUser);
        entityManager.persistAndFlush(inactiveUser);
    }

    @Test
    public void testFindByEmail() {
        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    public void testFindByEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    public void testFindByUsername() {
        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    public void testFindByUsernameNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    public void testExistsByEmail() {
        // When
        boolean exists = userRepository.existsByEmail("test@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void testExistsByUsername() {
        // When
        boolean exists = userRepository.existsByUsername("testuser");
        boolean notExists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void testFindActiveUserByEmail() {
        // When
        Optional<User> activeUser = userRepository.findActiveUserByEmail("test@example.com");
        Optional<User> inactiveUserResult = userRepository.findActiveUserByEmail("inactive@example.com");

        // Then
        assertThat(activeUser).isPresent();
        assertThat(activeUser.get().getIsActive()).isTrue();
        assertThat(inactiveUserResult).isEmpty();
    }

    @Test
    public void testFindActiveUserByUsername() {
        // When
        Optional<User> activeUser = userRepository.findActiveUserByUsername("testuser");
        Optional<User> inactiveUserResult = userRepository.findActiveUserByUsername("inactive");

        // Then
        assertThat(activeUser).isPresent();
        assertThat(activeUser.get().getIsActive()).isTrue();
        assertThat(inactiveUserResult).isEmpty();
    }

    @Test
    public void testExistsByEmailAndIdNot() {
        // When
        UUID testUserId = testUser.getId();
        boolean existsForDifferentUser = userRepository.existsByEmailAndIdNot("admin@example.com", testUserId);
        boolean existsForSameUser = userRepository.existsByEmailAndIdNot("test@example.com", testUserId);
        boolean existsForNonExistentEmail = userRepository.existsByEmailAndIdNot("nonexistent@example.com", testUserId);

        // Then
        assertThat(existsForDifferentUser).isTrue();
        assertThat(existsForSameUser).isFalse();
        assertThat(existsForNonExistentEmail).isFalse();
    }

    @Test
    public void testExistsByUsernameAndIdNot() {
        // When
        UUID testUserId = testUser.getId();
        boolean existsForDifferentUser = userRepository.existsByUsernameAndIdNot("admin", testUserId);
        boolean existsForSameUser = userRepository.existsByUsernameAndIdNot("testuser", testUserId);
        boolean existsForNonExistentUsername = userRepository.existsByUsernameAndIdNot("nonexistent", testUserId);

        // Then
        assertThat(existsForDifferentUser).isTrue();
        assertThat(existsForSameUser).isFalse();
        assertThat(existsForNonExistentUsername).isFalse();
    }

    @Test
    public void testSaveUser() {
        // Given
        User newUser = User.builder()
                .username("newuser")
                .email("new@example.com")
                .passwordHash("password")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();

        // When
        User saved = userRepository.save(newUser);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(userRepository.existsByEmail("new@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("newuser")).isTrue();
    }

    @Test
    public void testUpdateUser() {
        // Given
        User userToUpdate = userRepository.findByEmail("test@example.com").orElseThrow();
        userToUpdate.setFirstName("Updated");
        userToUpdate.setLastName("Name");

        // When
        User updated = userRepository.save(userToUpdate);

        // Then
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("Name");
        // Verify that updatedAt is not null and not before createdAt
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt().isAfter(updated.getCreatedAt()) || 
                   updated.getUpdatedAt().isEqual(updated.getCreatedAt())).isTrue();
    }

    @Test
    public void testDeleteUser() {
        // Given
        UUID userId = testUser.getId();

        // When
        userRepository.delete(testUser);

        // Then
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(userRepository.existsByEmail("test@example.com")).isFalse();
        assertThat(userRepository.existsByUsername("testuser")).isFalse();
    }

    @Test
    public void testFindAllUsers() {
        // When
        Iterable<User> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(3);
    }

    @Test
    public void testFindById() {
        // When
        Optional<User> found = userRepository.findById(testUser.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        Optional<User> found = userRepository.findById(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    public void testCountUsers() {
        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    public void testUserWithDifferentRoles() {
        // When
        Optional<User> userRole = userRepository.findByUsername("testuser");
        Optional<User> adminRole = userRepository.findByUsername("admin");

        // Then
        assertThat(userRole).isPresent();
        assertThat(userRole.get().getRole()).isEqualTo(UserRole.USER);
        assertThat(adminRole).isPresent();
        assertThat(adminRole.get().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    public void testUserWithLastLogin() {
        // Given
        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        LocalDateTime loginTime = LocalDateTime.now();

        // When
        user.setLastLogin(loginTime);
        User updated = userRepository.save(user);

        // Then
        assertThat(updated.getLastLogin()).isEqualTo(loginTime);
    }

    @Test
    public void testUserVerificationStatus() {
        // When
        Optional<User> verifiedUser = userRepository.findByEmail("admin@example.com");
        Optional<User> unverifiedUser = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(verifiedUser).isPresent();
        assertThat(verifiedUser.get().getIsVerified()).isTrue();
        assertThat(unverifiedUser).isPresent();
        assertThat(unverifiedUser.get().getIsVerified()).isFalse();
    }
}
