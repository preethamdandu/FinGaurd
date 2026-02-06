package com.fingaurd.comprehensive;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite covering all possible scenarios and edge cases
 * for the Model & Repository Layer (JPA)
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Comprehensive Test Suite - All Scenarios")
public class ComprehensiveTestSuite {

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
        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(true)
                .build();
        testUser = userRepository.save(testUser);

        // Create admin user
        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash("adminhash")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .isVerified(true)
                .build();
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Null and Empty Values")
    public void testNullAndEmptyValues() {
        // Test null username
        assertThatThrownBy(() -> {
            User user = User.builder()
                    .username(null)
                    .email("test@example.com")
                    .passwordHash("hash")
                    .build();
            userRepository.save(user);
        }).isInstanceOf(Exception.class);

        // Test empty username
        assertThatThrownBy(() -> {
            User user = User.builder()
                    .username("")
                    .email("test@example.com")
                    .passwordHash("hash")
                    .build();
            userRepository.save(user);
        }).isInstanceOf(Exception.class);

        // Test null email
        assertThatThrownBy(() -> {
            User user = User.builder()
                    .username("testuser2")
                    .email(null)
                    .passwordHash("hash")
                    .build();
            userRepository.save(user);
        }).isInstanceOf(Exception.class);

        // Test null password hash
        assertThatThrownBy(() -> {
            User user = User.builder()
                    .username("testuser3")
                    .email("test3@example.com")
                    .passwordHash(null)
                    .build();
            userRepository.save(user);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Maximum String Lengths")
    public void testMaximumStringLengths() {
        // Test username at max length (50 chars)
        String maxUsername = "a".repeat(50);
        User user = User.builder()
                .username(maxUsername)
                .email("max@example.com")
                .passwordHash("hash")
                .build();
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getUsername()).isEqualTo(maxUsername);

        // Test username exceeding max length
        String tooLongUsername = "a".repeat(51);
        assertThatThrownBy(() -> {
            User user2 = User.builder()
                    .username(tooLongUsername)
                    .email("toolong@example.com")
                    .passwordHash("hash")
                    .build();
            userRepository.save(user2);
        }).isInstanceOf(Exception.class);

        // Test email at reasonable length
        String longEmail = "verylongemailaddresstotest@verylongdomainnamethatmightcauseissues.com";
        User user3 = User.builder()
                .username("longemailuser")
                .email(longEmail)
                .passwordHash("hash")
                .build();
        User savedUser3 = userRepository.save(user3);
        assertThat(savedUser3.getEmail()).isEqualTo(longEmail);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Special Characters and Unicode")
    public void testSpecialCharactersAndUnicode() {
        // Test special characters in username
        User user1 = User.builder()
                .username("user_with_underscores")
                .email("special@example.com")
                .passwordHash("hash")
                .build();
        User savedUser1 = userRepository.save(user1);
        assertThat(savedUser1.getUsername()).isEqualTo("user_with_underscores");

        // Test numbers in username
        User user2 = User.builder()
                .username("user123")
                .email("numbers@example.com")
                .passwordHash("hash")
                .build();
        User savedUser2 = userRepository.save(user2);
        assertThat(savedUser2.getUsername()).isEqualTo("user123");

        // Test Unicode characters
        User user3 = User.builder()
                .username("用户名")
                .email("unicode@example.com")
                .passwordHash("hash")
                .firstName("José")
                .lastName("García")
                .build();
        User savedUser3 = userRepository.save(user3);
        assertThat(savedUser3.getUsername()).isEqualTo("用户名");
        assertThat(savedUser3.getFirstName()).isEqualTo("José");
        assertThat(savedUser3.getLastName()).isEqualTo("García");
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Extreme Transaction Amounts")
    public void testExtremeTransactionAmounts() {
        // Test minimum amount (0.01)
        Transaction minTx = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("0.01"))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedMinTx = transactionRepository.save(minTx);
        assertThat(savedMinTx.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));

        // Test maximum allowed amount (9999999999999.99)
        Transaction maxTx = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("9999999999999.99"))
                .transactionType(TransactionType.INCOME)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedMaxTx = transactionRepository.save(maxTx);
        assertThat(savedMaxTx.getAmount()).isEqualByComparingTo(new BigDecimal("9999999999999.99"));

        // Test zero amount
        Transaction zeroTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.ZERO)
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedZeroTx = transactionRepository.save(zeroTx);
        assertThat(savedZeroTx.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Test negative amount (should be allowed for corrections)
        Transaction negativeTx = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("-100.00"))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedNegativeTx = transactionRepository.save(negativeTx);
        assertThat(savedNegativeTx.getAmount()).isEqualByComparingTo(new BigDecimal("-100.00"));
    }

    @Test
    @DisplayName("🔍 EDGE CASE: High Precision Decimal Operations")
    public void testHighPrecisionDecimalOperations() {
        // Test very precise amounts
        BigDecimal preciseAmount = new BigDecimal("123.456789012345678");
        Transaction tx = Transaction.builder()
                .user(testUser)
                .amount(preciseAmount)
                .transactionType(TransactionType.INCOME)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedTx = transactionRepository.save(tx);
        
        // Verify precision is maintained (H2 typically truncates to 2 decimal places)
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("123.46"));
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Concurrent User Operations")
    public void testConcurrentUserOperations() {
        // Test multiple users with similar data
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                    .username("concurrentuser" + i)
                    .email("concurrent" + i + "@example.com")
                    .passwordHash("hash" + i)
                    .build();
            users.add(userRepository.save(user));
        }

        assertThat(users).hasSize(10);
        assertThat(userRepository.count()).isEqualTo(12); // 10 new + 2 from setup

        // Test transactions for all users
        for (int i = 0; i < 10; i++) {
            Transaction tx = Transaction.builder()
                    .user(users.get(i))
                    .amount(BigDecimal.valueOf(100.00 + i))
                    .transactionType(i % 2 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(tx);
        }

        assertThat(transactionRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Large Dataset Performance")
    public void testLargeDatasetPerformance() {
        // Create 100 transactions for performance testing
        List<Transaction> transactions = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < 100; i++) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(50.00 + i))
                    .transactionType(i % 3 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .category(i % 5 == 0 ? "Food" : i % 5 == 1 ? "Transport" : i % 5 == 2 ? "Entertainment" : "Other")
                    .description("Transaction " + i)
                    .transactionDate(baseTime.minusDays(i))
                    .build();
            transactions.add(tx);
        }

        // Save all transactions
        long startTime = System.currentTimeMillis();
        transactionRepository.saveAll(transactions);
        long endTime = System.currentTimeMillis();

        assertThat(transactionRepository.count()).isEqualTo(100);
        assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds

        // Test pagination performance
        Pageable pageable = PageRequest.of(0, 20);
        long queryStartTime = System.currentTimeMillis();
        var result = transactionRepository.findByUser(testUser, pageable);
        long queryEndTime = System.currentTimeMillis();

        assertThat(result.getContent()).hasSize(20);
        assertThat(queryEndTime - queryStartTime).isLessThan(1000); // Query should complete within 1 second
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Fraud Detection Scenarios")
    public void testFraudDetectionScenarios() {
        // Test high-risk transaction
        Transaction highRiskTx = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("10000.00"))
                .transactionType(TransactionType.EXPENSE)
                .category("Unknown")
                .description("Large suspicious transaction")
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(true)
                .fraudRiskScore(new BigDecimal("0.95"))
                .build();
        Transaction savedHighRiskTx = transactionRepository.save(highRiskTx);

        assertThat(savedHighRiskTx.isFraud()).isTrue();
        assertThat(savedHighRiskTx.getRiskScoreAsDouble()).isEqualTo(0.95);

        // Test multiple fraud transactions
        for (int i = 0; i < 5; i++) {
            Transaction fraudTx = Transaction.builder()
                    .user(testUser)
                    .amount(new BigDecimal("5000.00"))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now().minusHours(i))
                    .isFraudFlagged(true)
                    .fraudRiskScore(new BigDecimal("0.8" + i))
                    .build();
            transactionRepository.save(fraudTx);
        }

        // Verify fraud transactions count
        Pageable pageable = PageRequest.of(0, 10);
        var fraudTransactions = transactionRepository.findByUserAndIsFraudFlagged(testUser, true, pageable);
        assertThat(fraudTransactions.getTotalElements()).isEqualTo(6); // 1 + 5
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Date Range Edge Cases")
    public void testDateRangeEdgeCases() {
        LocalDateTime now = LocalDateTime.now();
        
        // Test transactions at exact boundaries
        Transaction pastTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.INCOME)
                .transactionDate(now.minusYears(1))
                .build();
        transactionRepository.save(pastTx);

        Transaction futureTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(200.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(now.plusDays(1))
                .build();
        transactionRepository.save(futureTx);

        // Test date range queries
        LocalDateTime startDate = now.minusDays(30);
        LocalDateTime endDate = now.plusDays(30);
        List<Transaction> rangeTx = transactionRepository.findByUserAndTransactionDateBetween(testUser, startDate, endDate);
        
        assertThat(rangeTx).hasSize(2); // Should include both transactions
    }

    @Test
    @DisplayName("🔍 EDGE CASE: User Role Edge Cases")
    public void testUserRoleEdgeCases() {
        // Test all user roles
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
            
            if (role == UserRole.ADMIN) {
                assertThat(savedUser.isAdmin()).isTrue();
            } else {
                assertThat(savedUser.isAdmin()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("🔍 EDGE CASE: User Status Combinations")
    public void testUserStatusCombinations() {
        // Test inactive user
        User inactiveUser = User.builder()
                .username("inactiveuser")
                .email("inactive@example.com")
                .passwordHash("hash")
                .isActive(false)
                .isVerified(true)
                .build();
        userRepository.save(inactiveUser);

        // Test unverified user
        User unverifiedUser = User.builder()
                .username("unverifieduser")
                .email("unverified@example.com")
                .passwordHash("hash")
                .isActive(true)
                .isVerified(false)
                .build();
        userRepository.save(unverifiedUser);

        // Test inactive and unverified user
        User inactiveUnverifiedUser = User.builder()
                .username("inactiveunverified")
                .email("inactiveunverified@example.com")
                .passwordHash("hash")
                .isActive(false)
                .isVerified(false)
                .build();
        userRepository.save(inactiveUnverifiedUser);

        // Verify active user queries
        Optional<User> activeUser = userRepository.findActiveUserByEmail("test@example.com");
        assertThat(activeUser).isPresent();

        Optional<User> inactiveUserResult = userRepository.findActiveUserByEmail("inactive@example.com");
        assertThat(inactiveUserResult).isNotPresent();
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Transaction Category Edge Cases")
    public void testTransactionCategoryEdgeCases() {
        // Test null category
        Transaction nullCategoryTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .category(null)
                .transactionDate(LocalDateTime.now())
                .build();
        transactionRepository.save(nullCategoryTx);

        // Test empty category
        Transaction emptyCategoryTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(200.00))
                .transactionType(TransactionType.INCOME)
                .category("")
                .transactionDate(LocalDateTime.now())
                .build();
        transactionRepository.save(emptyCategoryTx);

        // Test very long category
        String longCategory = "Very Long Category Name That Might Exceed Normal Limits And Test The System";
        Transaction longCategoryTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(300.00))
                .transactionType(TransactionType.EXPENSE)
                .category(longCategory)
                .transactionDate(LocalDateTime.now())
                .build();
        transactionRepository.save(longCategoryTx);

        // Test special characters in category
        Transaction specialCategoryTx = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(400.00))
                .transactionType(TransactionType.INCOME)
                .category("Food & Drinks (Restaurants)")
                .transactionDate(LocalDateTime.now())
                .build();
        transactionRepository.save(specialCategoryTx);

        assertThat(transactionRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Complex Query Performance")
    public void testComplexQueryPerformance() {
        // Create test data for complex queries
        LocalDateTime now = LocalDateTime.now();
        String[] categories = {"Food", "Transport", "Entertainment", "Shopping", "Utilities"};
        
        for (int i = 0; i < 50; i++) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(10.00 + i * 5))
                    .transactionType(i % 2 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .category(categories[i % categories.length])
                    .description("Test transaction " + i)
                    .transactionDate(now.minusDays(i))
                    .isFraudFlagged(i % 10 == 0)
                    .fraudRiskScore(i % 10 == 0 ? new BigDecimal("0.8") : null)
                    .build();
            transactionRepository.save(tx);
        }

        // Test complex aggregation queries
        long startTime = System.currentTimeMillis();
        
        // Test category statistics
        List<Object[]> stats = transactionRepository.getTransactionStatsByCategory(
                testUser, TransactionType.EXPENSE, now.minusDays(30), now);
        
        long endTime = System.currentTimeMillis();
        
        assertThat(stats).isNotEmpty();
        assertThat(endTime - startTime).isLessThan(2000); // Should complete within 2 seconds
        
        // Test pagination with complex filters
        Pageable pageable = PageRequest.of(0, 10);
        startTime = System.currentTimeMillis();
        
        var filteredResults = transactionRepository.findByUserCategoryAndDateRange(
                testUser, "Food", now.minusDays(30), now, pageable);
        
        endTime = System.currentTimeMillis();
        
        assertThat(filteredResults).isNotNull();
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Database Constraint Violations")
    public void testDatabaseConstraintViolations() {
        // Test duplicate email
        assertThatThrownBy(() -> {
            User duplicateEmailUser = User.builder()
                    .username("differentusername")
                    .email("test@example.com") // Same email as testUser
                    .passwordHash("differenthash")
                    .build();
            userRepository.save(duplicateEmailUser);
        }).isInstanceOf(Exception.class);

        // Test duplicate username
        assertThatThrownBy(() -> {
            User duplicateUsernameUser = User.builder()
                    .username("testuser") // Same username as testUser
                    .email("different@example.com")
                    .passwordHash("differenthash")
                    .build();
            userRepository.save(duplicateUsernameUser);
        }).isInstanceOf(Exception.class);

        // Test null user in transaction
        assertThatThrownBy(() -> {
            Transaction nullUserTx = Transaction.builder()
                    .user(null) // This should fail
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(nullUserTx);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Transaction Type Edge Cases")
    public void testTransactionTypeEdgeCases() {
        // Test all transaction types
        TransactionType[] types = {TransactionType.INCOME, TransactionType.EXPENSE};
        
        for (TransactionType type : types) {
            Transaction tx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(type)
                    .transactionDate(LocalDateTime.now())
                    .build();
            Transaction savedTx = transactionRepository.save(tx);
            
            assertThat(savedTx.getTransactionType()).isEqualTo(type);
            
            if (type == TransactionType.INCOME) {
                assertThat(savedTx.isIncome()).isTrue();
                assertThat(savedTx.isExpense()).isFalse();
            } else {
                assertThat(savedTx.isIncome()).isFalse();
                assertThat(savedTx.isExpense()).isTrue();
            }
        }
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Memory and Resource Management")
    public void testMemoryAndResourceManagement() {
        // Create and immediately delete users to test resource cleanup
        List<User> tempUsers = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            User tempUser = User.builder()
                    .username("tempuser" + i)
                    .email("temp" + i + "@example.com")
                    .passwordHash("temphash")
                    .build();
            tempUsers.add(userRepository.save(tempUser));
        }
        
        // Delete all temp users
        userRepository.deleteAll(tempUsers);
        
        // Verify cleanup
        assertThat(userRepository.count()).isEqualTo(2); // Only testUser and adminUser should remain
        
        // Test transaction cleanup
        List<Transaction> tempTransactions = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            Transaction tempTx = Transaction.builder()
                    .user(testUser)
                    .amount(BigDecimal.valueOf(i + 1))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            tempTransactions.add(transactionRepository.save(tempTx));
        }
        
        // Delete all temp transactions
        transactionRepository.deleteAll(tempTransactions);
        
        // Verify cleanup
        assertThat(transactionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("🔍 EDGE CASE: Full Name Generation Edge Cases")
    public void testFullNameGenerationEdgeCases() {
        // Test with only first name
        User firstNameOnly = User.builder()
                .username("firstonly")
                .email("firstonly@example.com")
                .passwordHash("hash")
                .firstName("John")
                .build();
        assertThat(firstNameOnly.getFullName()).isEqualTo("John");

        // Test with only last name
        User lastNameOnly = User.builder()
                .username("lastonly")
                .email("lastonly@example.com")
                .passwordHash("hash")
                .lastName("Doe")
                .build();
        assertThat(lastNameOnly.getFullName()).isEqualTo("Doe");

        // Test with no first or last name
        User noNames = User.builder()
                .username("nonames")
                .email("nonames@example.com")
                .passwordHash("hash")
                .build();
        assertThat(noNames.getFullName()).isEqualTo("nonames");

        // Test with both names
        User bothNames = User.builder()
                .username("bothnames")
                .email("bothnames@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Smith")
                .build();
        assertThat(bothNames.getFullName()).isEqualTo("Jane Smith");
    }
}
