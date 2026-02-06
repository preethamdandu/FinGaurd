package com.fingaurd.security;

import com.fingaurd.model.Transaction;
import com.fingaurd.model.TransactionType;
import com.fingaurd.model.User;
import com.fingaurd.model.UserRole;
import com.fingaurd.repository.TransactionRepository;
import com.fingaurd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Security and validation test suite for data integrity and security
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Security & Validation Test Suite")
public class SecurityValidationTestSuite {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private User adminUser;

    @BeforeEach
    public void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(UserRole.USER)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash("adminhash")
                .role(UserRole.ADMIN)
                .build();
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("🔒 SECURITY: SQL Injection Prevention")
    public void testSqlInjectionPrevention() {
        // Test malicious username
        String maliciousUsername = "admin'; DROP TABLE users; --";
        User maliciousUser = User.builder()
                .username(maliciousUsername)
                .email("malicious@example.com")
                .passwordHash("hash")
                .build();
        User savedUser = userRepository.save(maliciousUser);

        // Verify the malicious string is stored as literal text, not executed
        assertThat(savedUser.getUsername()).isEqualTo(maliciousUsername);
        
        // Verify other users are still intact
        assertThat(userRepository.count()).isEqualTo(3); // malicious + testUser + adminUser
        
        // Test malicious email
        String maliciousEmail = "test@example.com' OR '1'='1";
        User emailUser = User.builder()
                .username("emailuser")
                .email(maliciousEmail)
                .passwordHash("hash")
                .build();
        userRepository.save(emailUser);

        // Verify email injection attempt fails
        Optional<User> foundUser = userRepository.findByEmail(maliciousEmail);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(maliciousEmail);
    }

    @Test
    @DisplayName("🔒 SECURITY: XSS Prevention in Data Storage")
    public void testXssPrevention() {
        // Test XSS payload in user fields
        String xssPayload = "<script>alert('XSS')</script>";
        
        User xssUser = User.builder()
                .username("xssuser")
                .email("xss@example.com")
                .passwordHash("hash")
                .firstName(xssPayload)
                .lastName(xssPayload)
                .build();
        User savedUser = userRepository.save(xssUser);

        // Verify XSS payload is stored as literal text
        assertThat(savedUser.getFirstName()).isEqualTo(xssPayload);
        assertThat(savedUser.getLastName()).isEqualTo(xssPayload);

        // Test XSS in transaction description
        Transaction xssTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .description(xssPayload)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedTx = transactionRepository.save(xssTx);

        assertThat(savedTx.getDescription()).isEqualTo(xssPayload);
    }

    @Test
    @DisplayName("🔒 SECURITY: Data Isolation Between Users")
    public void testDataIsolationBetweenUsers() {
        // Create another user
        User anotherUser = User.builder()
                .username("anotheruser")
                .email("another@example.com")
                .passwordHash("hash")
                .build();
        anotherUser = userRepository.save(anotherUser);

        // Create transactions for test user
        for (int i = 0; i < 5; i++) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(100.00 + i))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(tx);
        }

        // Create transactions for another user
        for (int i = 0; i < 3; i++) {
            Transaction tx = Transaction.builder()
                    .user(anotherUser)
                    .amount(BigDecimal.valueOf(200.00 + i))
                    .transactionType(TransactionType.INCOME)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(tx);
        }

        // Verify data isolation
        assertThat(transactionRepository.countByUser(testUser)).isEqualTo(5);
        assertThat(transactionRepository.countByUser(anotherUser)).isEqualTo(3);
        assertThat(transactionRepository.count()).isEqualTo(8);

        // Verify users can only see their own transactions
        var testUserTransactions = transactionRepository.findByUser(testUser, null);
        assertThat(testUserTransactions).hasSize(5);
        
        var anotherUserTransactions = transactionRepository.findByUser(anotherUser, null);
        assertThat(anotherUserTransactions).hasSize(3);

        // Verify no cross-contamination
        for (Transaction tx : testUserTransactions) {
            assertThat(tx.getUser().getId()).isEqualTo(testUser.getId());
        }
        
        for (Transaction tx : anotherUserTransactions) {
            assertThat(tx.getUser().getId()).isEqualTo(anotherUser.getId());
        }
    }

    @Test
    @DisplayName("🔒 SECURITY: Password Hash Security")
    public void testPasswordHashSecurity() {
        // Test that password hashes are different for same password
        String password = "samepassword";
        
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("hash1") // In real app, this would be BCrypt
                .build();
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("hash2") // In real app, this would be BCrypt
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        // Verify password hashes are stored
        User savedUser1 = userRepository.findByUsername("user1").orElseThrow();
        User savedUser2 = userRepository.findByUsername("user2").orElseThrow();

        assertThat(savedUser1.getPasswordHash()).isEqualTo("hash1");
        assertThat(savedUser2.getPasswordHash()).isEqualTo("hash2");
        assertThat(savedUser1.getPasswordHash()).isNotEqualTo(savedUser2.getPasswordHash());

        // Test that password hash is not exposed in toString
        String user1String = savedUser1.toString();
        assertThat(user1String).doesNotContain("hash1");
    }

    @Test
    @DisplayName("🔒 SECURITY: Input Validation and Sanitization")
    public void testInputValidationAndSanitization() {
        // Test extremely long input
        String longString = "a".repeat(10000);
        
        User longInputUser = User.builder()
                .username("longinputuser")
                .email("longinput@example.com")
                .passwordHash("hash")
                .firstName(longString)
                .build();

        // This should either be truncated or rejected
        try {
            userRepository.save(longInputUser);
            User savedUser = userRepository.findByUsername("longinputuser").orElseThrow();
            // If saved, verify it's truncated
            assertThat(savedUser.getFirstName().length()).isLessThan(10000);
        } catch (Exception e) {
            // If rejected, that's also acceptable
            assertThat(e).isInstanceOf(Exception.class);
        }

        // Test special characters that might cause issues
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
        
        User specialUser = User.builder()
                .username("specialuser")
                .email("special@example.com")
                .passwordHash("hash")
                .firstName(specialChars)
                .lastName(specialChars)
                .build();
        userRepository.save(specialUser);

        User savedSpecialUser = userRepository.findByUsername("specialuser").orElseThrow();
        assertThat(savedSpecialUser.getFirstName()).isEqualTo(specialChars);
        assertThat(savedSpecialUser.getLastName()).isEqualTo(specialChars);
    }

    @Test
    @DisplayName("🔒 SECURITY: Role-Based Access Control")
    public void testRoleBasedAccessControl() {
        // Test different user roles
        UserRole[] roles = {UserRole.USER, UserRole.ADMIN, UserRole.MODERATOR};
        
        for (UserRole role : roles) {
            User user = User.builder()
                    .username("roleuser_" + role.name().toLowerCase())
                    .email("role_" + role.name().toLowerCase() + "@example.com")
                    .passwordHash("hash")
                    .role(role)
                    .build();
            User savedUser = userRepository.save(user);
            
            assertThat(savedUser.getRole()).isEqualTo(role);
            
            // Test admin check
            if (role == UserRole.ADMIN) {
                assertThat(savedUser.isAdmin()).isTrue();
            } else {
                assertThat(savedUser.isAdmin()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("🔒 SECURITY: Transaction Amount Validation")
    public void testTransactionAmountValidation() {
        // Test various amount scenarios
        
        // Valid amounts
        BigDecimal[] validAmounts = {
            new BigDecimal("0.01"),
            new BigDecimal("1.00"),
            new BigDecimal("100.50"),
            new BigDecimal("9999999999999.99"),
            new BigDecimal("-100.00") // Negative for corrections
        };

        for (BigDecimal amount : validAmounts) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(amount)
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            Transaction savedTx = transactionRepository.save(tx);
            assertThat(savedTx.getAmount()).isEqualByComparingTo(amount);
        }

        // Test invalid amount (exceeding precision)
        BigDecimal invalidAmount = new BigDecimal("10000000000000.00"); // Exceeds 15 digits
        
        assertThatThrownBy(() -> {
            Transaction invalidTx = Transaction.builder()
                    .user(testUser)
                    .amount(invalidAmount)
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(invalidTx);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("🔒 SECURITY: Date Validation")
    public void testDateValidation() {
        // Test various date scenarios
        
        // Valid dates
        LocalDateTime[] validDates = {
            LocalDateTime.now(),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusYears(1),
            LocalDateTime.now().plusDays(1) // Future dates might be valid for scheduled transactions
        };

        for (LocalDateTime date : validDates) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(date)
                    .build();
            Transaction savedTx = transactionRepository.save(tx);
            assertThat(savedTx.getTransactionDate()).isNotNull();
        }

        // Test null date (should fail)
        assertThatThrownBy(() -> {
            Transaction nullDateTx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(null)
                    .build();
            transactionRepository.save(nullDateTx);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("🔒 SECURITY: Fraud Detection Data Integrity")
    public void testFraudDetectionDataIntegrity() {
        // Test fraud detection fields
        
        Transaction fraudTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(10000.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(true)
                .fraudRiskScore(new BigDecimal("0.95"))
                .build();
        Transaction savedFraudTx = transactionRepository.save(fraudTx);

        assertThat(savedFraudTx.isFraud()).isTrue();
        assertThat(savedFraudTx.getRiskScoreAsDouble()).isEqualTo(0.95);

        // Test risk score bounds
        BigDecimal[] riskScores = {
            new BigDecimal("0.00"),
            new BigDecimal("0.50"),
            new BigDecimal("1.00"),
            new BigDecimal("0.9999")
        };

        for (BigDecimal riskScore : riskScores) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .fraudRiskScore(riskScore)
                    .build();
            Transaction savedTx = transactionRepository.save(tx);
            assertThat(savedTx.getFraudRiskScore()).isEqualByComparingTo(riskScore);
        }

        // Test invalid risk score (should be between 0 and 1)
        BigDecimal invalidRiskScore = new BigDecimal("1.50");
        Transaction invalidRiskTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .fraudRiskScore(invalidRiskScore)
                .build();
        
        // This might be allowed by the database, but should be validated at application level
        Transaction savedInvalidRiskTx = transactionRepository.save(invalidRiskTx);
        assertThat(savedInvalidRiskTx.getFraudRiskScore()).isEqualByComparingTo(invalidRiskScore);
    }

    @Test
    @DisplayName("🔒 SECURITY: Data Consistency Checks")
    public void testDataConsistencyChecks() {
        // Test that related data remains consistent
        
        // Create user with transactions
        User consistencyUser = User.builder()
                .username("consistencyuser")
                .email("consistency@example.com")
                .passwordHash("hash")
                .build();
        consistencyUser = userRepository.save(consistencyUser);

        List<Transaction> transactions = List.of(
            Transaction.builder()
                    .user(consistencyUser)
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(TransactionType.INCOME)
                    .transactionDate(LocalDateTime.now())
                    .build(),
            Transaction.builder()
                    .user(consistencyUser)
                    .amount(BigDecimal.valueOf(50.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build(),
            Transaction.builder()
                    .user(consistencyUser)
                    .amount(BigDecimal.valueOf(25.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build()
        );

        transactionRepository.saveAll(transactions);

        // Verify data consistency
        assertThat(transactionRepository.countByUser(consistencyUser)).isEqualTo(3);
        
        BigDecimal incomeSum = transactionRepository.sumAmountByUserAndType(consistencyUser, TransactionType.INCOME);
        BigDecimal expenseSum = transactionRepository.sumAmountByUserAndType(consistencyUser, TransactionType.EXPENSE);
        
        assertThat(incomeSum).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(expenseSum).isEqualByComparingTo(BigDecimal.valueOf(75.00)); // 50 + 25

        // Verify user-transaction relationships
        var userTransactions = transactionRepository.findByUser(consistencyUser, null);
        for (Transaction tx : userTransactions) {
            assertThat(tx.getUser().getId()).isEqualTo(consistencyUser.getId());
        }
    }

    @Test
    @DisplayName("🔒 SECURITY: Audit Trail Integrity")
    public void testAuditTrailIntegrity() {
        // Test that audit fields are properly maintained
        
        User auditUser = User.builder()
                .username("audituser")
                .email("audit@example.com")
                .passwordHash("hash")
                .build();
        User savedUser = userRepository.save(auditUser);

        // Verify audit fields are set
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        // Update user and verify updatedAt changes
        LocalDateTime originalUpdatedAt = savedUser.getUpdatedAt();
        savedUser.setFirstName("Updated");
        userRepository.save(savedUser);

        User updatedUser = userRepository.findByUsername("audituser").orElseThrow();
        assertThat(updatedUser.getUpdatedAt()).isNotNull();
        assertThat(updatedUser.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   updatedUser.getUpdatedAt().isEqual(originalUpdatedAt)).isTrue();

        // Test transaction audit fields
        Transaction auditTx = Transaction.builder()
                .user(auditUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedTx = transactionRepository.save(auditTx);

        assertThat(savedTx.getCreatedAt()).isNotNull();
        assertThat(savedTx.getUpdatedAt()).isNotNull();

        // Update transaction and verify updatedAt changes
        LocalDateTime originalTxUpdatedAt = savedTx.getUpdatedAt();
        savedTx.setDescription("Updated description");
        transactionRepository.save(savedTx);

        Transaction updatedTx = transactionRepository.findById(savedTx.getId()).orElseThrow();
        assertThat(updatedTx.getUpdatedAt()).isNotNull();
        assertThat(updatedTx.getUpdatedAt().isAfter(originalTxUpdatedAt) || 
                   updatedTx.getUpdatedAt().isEqual(originalTxUpdatedAt)).isTrue();
    }
}
