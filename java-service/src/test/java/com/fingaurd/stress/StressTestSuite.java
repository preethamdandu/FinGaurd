package com.fingaurd.stress;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Stress test suite for extreme scenarios and performance testing
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Stress Test Suite - Extreme Scenarios")
public class StressTestSuite {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    public void setUp() {
        // Minimal setup for stress tests
    }

    @Test
    @DisplayName("🔥 STRESS: Bulk User Creation (1000 users)")
    public void testBulkUserCreation() {
        List<User> users = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Create 1000 users
        for (int i = 0; i < 1000; i++) {
            User user = User.builder()
                    .username("stressuser" + i)
                    .email("stress" + i + "@example.com")
                    .passwordHash("streshash" + i)
                    .firstName("Stress" + i)
                    .lastName("User" + i)
                    .role(i % 10 == 0 ? UserRole.ADMIN : UserRole.USER)
                    .isActive(i % 100 != 0) // Make some users inactive
                    .isVerified(i % 50 != 0) // Make some users unverified
                    .build();
            users.add(user);
        }

        // Save all users in batches
        userRepository.saveAll(users);
        long endTime = System.currentTimeMillis();

        assertThat(userRepository.count()).isEqualTo(1000);
        assertThat(endTime - startTime).isLessThan(10000); // Should complete within 10 seconds

        // Verify data integrity
        User firstUser = userRepository.findByUsername("stressuser0").orElseThrow();
        User lastUser = userRepository.findByUsername("stressuser999").orElseThrow();
        
        assertThat(firstUser.getUsername()).isEqualTo("stressuser0");
        assertThat(lastUser.getUsername()).isEqualTo("stressuser999");
    }

    @Test
    @DisplayName("🔥 STRESS: Massive Transaction Dataset (5000 transactions)")
    public void testMassiveTransactionDataset() {
        // Create a test user first
        User stressUser = User.builder()
                .username("stressuser")
                .email("stress@example.com")
                .passwordHash("streshash")
                .build();
        stressUser = userRepository.save(stressUser);

        List<Transaction> transactions = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();
        String[] categories = {"Food", "Transport", "Entertainment", "Shopping", "Utilities", "Healthcare", "Education"};

        long startTime = System.currentTimeMillis();

        // Create 5000 transactions
        for (int i = 0; i < 5000; i++) {
            Transaction tx = Transaction.builder()
                    .user(stressUser)
                    .amount(BigDecimal.valueOf(Math.random() * 10000).setScale(2, BigDecimal.ROUND_HALF_UP))
                    .transactionType(i % 2 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .category(categories[i % categories.length])
                    .description("Stress test transaction " + i)
                    .transactionDate(baseTime.minusDays(i % 365)) // Spread over a year
                    .isFraudFlagged(i % 100 == 0) // 1% fraud rate
                    .fraudRiskScore(i % 100 == 0 ? new BigDecimal(Math.random()).setScale(4, BigDecimal.ROUND_HALF_UP) : null)
                    .build();
            transactions.add(tx);
        }

        // Save all transactions
        transactionRepository.saveAll(transactions);
        long endTime = System.currentTimeMillis();

        assertThat(transactionRepository.count()).isEqualTo(5000);
        assertThat(endTime - startTime).isLessThan(15000); // Should complete within 15 seconds

        // Test complex queries on large dataset
        Pageable pageable = PageRequest.of(0, 100);
        long queryStartTime = System.currentTimeMillis();
        
        var result = transactionRepository.findByUser(stressUser, pageable);
        
        long queryEndTime = System.currentTimeMillis();
        
        assertThat(result.getTotalElements()).isEqualTo(5000);
        assertThat(result.getContent()).hasSize(100);
        assertThat(queryEndTime - queryStartTime).isLessThan(2000); // Query should complete within 2 seconds
    }

    @Test
    @DisplayName("🔥 STRESS: Concurrent Operations")
    public void testConcurrentOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Create users concurrently
        for (int i = 0; i < 50; i++) {
            final int userId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                User user = User.builder()
                        .username("concurrentuser" + userId)
                        .email("concurrent" + userId + "@example.com")
                        .passwordHash("concurrenthash" + userId)
                        .build();
                userRepository.save(user);
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Concurrent operations failed", e);
        }

        executor.shutdown();

        assertThat(userRepository.count()).isEqualTo(50);
    }

    @Test
    @DisplayName("🔥 STRESS: Memory-Intensive Operations")
    public void testMemoryIntensiveOperations() {
        User memoryUser = User.builder()
                .username("memoryuser")
                .email("memory@example.com")
                .passwordHash("memoryhash")
                .build();
        memoryUser = userRepository.save(memoryUser);

        // Create transactions with large descriptions
        List<Transaction> transactions = new ArrayList<>();
        String largeDescription = "This is a very large description that contains a lot of text to test memory usage. ".repeat(100);

        for (int i = 0; i < 100; i++) {
            Transaction tx = Transaction.builder()
                    .user(memoryUser)
                    .amount(BigDecimal.valueOf(100.00 + i))
                    .transactionType(TransactionType.EXPENSE)
                    .category("Memory Test")
                    .description(largeDescription + i)
                    .transactionDate(LocalDateTime.now().minusDays(i))
                    .build();
            transactions.add(tx);
        }

        transactionRepository.saveAll(transactions);
        assertThat(transactionRepository.count()).isEqualTo(100);

        // Test retrieval of large descriptions
        Pageable pageable = PageRequest.of(0, 10);
        var result = transactionRepository.findByUser(memoryUser, pageable);
        
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getContent().get(0).getDescription()).contains("This is a very large description");
    }

    @Test
    @DisplayName("🔥 STRESS: Rapid CRUD Operations")
    public void testRapidCrudOperations() {
        User crudUser = User.builder()
                .username("cruduser")
                .email("crud@example.com")
                .passwordHash("crudhash")
                .build();
        crudUser = userRepository.save(crudUser);

        long startTime = System.currentTimeMillis();

        // Rapid create, read, update, delete operations
        for (int i = 0; i < 100; i++) {
            // Create
            Transaction tx = Transaction.builder()
                    .user(crudUser)
                    .amount(BigDecimal.valueOf(100.00 + i))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            Transaction savedTx = transactionRepository.save(tx);

            // Read
            Optional<Transaction> foundTx = transactionRepository.findById(savedTx.getId());
            assertThat(foundTx).isPresent();

            // Update
            savedTx.setAmount(BigDecimal.valueOf(200.00 + i));
            savedTx.setDescription("Updated transaction " + i);
            transactionRepository.save(savedTx);

            // Verify update
            Optional<Transaction> updatedTx = transactionRepository.findById(savedTx.getId());
            assertThat(updatedTx).isPresent();
            assertThat(updatedTx.get().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00 + i));
        }

        long endTime = System.currentTimeMillis();

        assertThat(transactionRepository.count()).isEqualTo(100);
        assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds
    }

    @Test
    @DisplayName("🔥 STRESS: Extreme Pagination")
    public void testExtremePagination() {
        User pageUser = User.builder()
                .username("pageuser")
                .email("page@example.com")
                .passwordHash("pagehash")
                .build();
        pageUser = userRepository.save(pageUser);

        // Create 1000 transactions for pagination testing
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Transaction tx = Transaction.builder()
                    .user(pageUser)
                    .amount(BigDecimal.valueOf(i))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now().minusDays(i))
                    .build();
            transactions.add(tx);
        }
        transactionRepository.saveAll(transactions);

        // Test pagination through all pages
        int pageSize = 50;
        int totalPages = 1000 / pageSize;
        long startTime = System.currentTimeMillis();

        for (int page = 0; page < totalPages; page++) {
            Pageable pageable = PageRequest.of(page, pageSize);
            var result = transactionRepository.findByUser(pageUser, pageable);
            
            assertThat(result.getContent()).hasSize(pageSize);
            assertThat(result.getTotalElements()).isEqualTo(1000);
            assertThat(result.getTotalPages()).isEqualTo(totalPages);
            assertThat(result.getNumber()).isEqualTo(page);
        }

        long endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isLessThan(3000); // Should complete within 3 seconds
    }

    @Test
    @DisplayName("🔥 STRESS: Complex Aggregation Queries")
    public void testComplexAggregationQueries() {
        User aggUser = User.builder()
                .username("agguser")
                .email("agg@example.com")
                .passwordHash("agghash")
                .build();
        aggUser = userRepository.save(aggUser);

        LocalDateTime now = LocalDateTime.now();
        String[] categories = {"Food", "Transport", "Entertainment", "Shopping", "Utilities"};

        // Create diverse transaction data
        for (int i = 0; i < 500; i++) {
            Transaction tx = Transaction.builder()
                    .user(aggUser)
                    .amount(BigDecimal.valueOf(10 + (i % 100) * 5))
                    .transactionType(i % 3 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .category(categories[i % categories.length])
                    .transactionDate(now.minusDays(i % 30))
                    .build();
            transactionRepository.save(tx);
        }

        // Test multiple aggregation queries
        long startTime = System.currentTimeMillis();

        // Test category statistics
        List<Object[]> categoryStats = transactionRepository.getTransactionStatsByCategory(
                aggUser, TransactionType.EXPENSE, now.minusDays(30), now);

        // Test sum queries
        BigDecimal incomeSum = transactionRepository.sumAmountByUserAndType(aggUser, TransactionType.INCOME);
        BigDecimal expenseSum = transactionRepository.sumAmountByUserAndType(aggUser, TransactionType.EXPENSE);

        // Test count queries
        long incomeCount = transactionRepository.countByUserAndTransactionType(aggUser, TransactionType.INCOME);
        long expenseCount = transactionRepository.countByUserAndTransactionType(aggUser, TransactionType.EXPENSE);

        long endTime = System.currentTimeMillis();

        assertThat(categoryStats).isNotEmpty();
        assertThat(incomeSum).isNotNull();
        assertThat(expenseSum).isNotNull();
        assertThat(incomeCount).isGreaterThan(0);
        assertThat(expenseCount).isGreaterThan(0);
        assertThat(endTime - startTime).isLessThan(2000); // Should complete within 2 seconds
    }

    @Test
    @DisplayName("🔥 STRESS: Database Connection Stress")
    public void testDatabaseConnectionStress() {
        // Test rapid connection/disconnection scenarios
        List<User> users = new ArrayList<>();
        
        for (int batch = 0; batch < 10; batch++) {
            List<User> batchUsers = new ArrayList<>();
            
            for (int i = 0; i < 10; i++) {
                User user = User.builder()
                        .username("batchuser" + batch + "_" + i)
                        .email("batch" + batch + "_" + i + "@example.com")
                        .passwordHash("batchhash" + batch + "_" + i)
                        .build();
                batchUsers.add(user);
            }
            
            // Save batch
            userRepository.saveAll(batchUsers);
            users.addAll(batchUsers);
            
            // Clear entity manager to simulate connection reset
            entityManager.clear();
        }

        assertThat(userRepository.count()).isEqualTo(100);
        
        // Verify data is still accessible after clear
        Optional<User> firstUser = userRepository.findByUsername("batchuser0_0");
        Optional<User> lastUser = userRepository.findByUsername("batchuser9_9");
        
        assertThat(firstUser).isPresent();
        assertThat(lastUser).isPresent();
    }

    @Test
    @DisplayName("🔥 STRESS: Transaction Rollback Scenarios")
    public void testTransactionRollbackScenarios() {
        // Test that transactions are properly rolled back on failure
        User rollbackUser = User.builder()
                .username("rollbackuser")
                .email("rollback@example.com")
                .passwordHash("rollbackhash")
                .build();
        rollbackUser = userRepository.save(rollbackUser);

        // Create some valid transactions
        for (int i = 0; i < 5; i++) {
            Transaction tx = Transaction.builder()
                    .user(rollbackUser)
                    .amount(BigDecimal.valueOf(100.00 + i))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(tx);
        }

        // Verify initial state
        assertThat(transactionRepository.count()).isEqualTo(5);

        // Simulate a rollback scenario by creating an invalid transaction
        // (This test verifies that the test framework properly handles rollbacks)
        assertThatThrownBy(() -> {
            Transaction invalidTx = Transaction.builder()
                    .user(null) // This should cause a rollback
                    .amount(BigDecimal.valueOf(100.00))
                    .transactionType(TransactionType.EXPENSE)
                    .transactionDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(invalidTx);
        }).isInstanceOf(Exception.class);

        // Verify that the valid transactions are still there (rollback worked)
        assertThat(transactionRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("🔥 STRESS: Boundary Value Testing")
    public void testBoundaryValueTesting() {
        // Test boundary values for all numeric fields
        
        // Test maximum allowed amount
        User boundaryUser = User.builder()
                .username("boundaryuser")
                .email("boundary@example.com")
                .passwordHash("boundaryhash")
                .build();
        boundaryUser = userRepository.save(boundaryUser);

        // Test maximum precision amount
        BigDecimal maxAmount = new BigDecimal("9999999999999.99");
        Transaction maxTx = Transaction.builder()
                .user(boundaryUser)
                .amount(maxAmount)
                .transactionType(TransactionType.INCOME)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedMaxTx = transactionRepository.save(maxTx);
        assertThat(savedMaxTx.getAmount()).isEqualByComparingTo(maxAmount);

        // Test minimum amount
        BigDecimal minAmount = new BigDecimal("0.01");
        Transaction minTx = Transaction.builder()
                .user(boundaryUser)
                .amount(minAmount)
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedMinTx = transactionRepository.save(minTx);
        assertThat(savedMinTx.getAmount()).isEqualByComparingTo(minAmount);

        // Test zero amount
        Transaction zeroTx = Transaction.builder()
                .user(boundaryUser)
                .amount(BigDecimal.ZERO)
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedZeroTx = transactionRepository.save(zeroTx);
        assertThat(savedZeroTx.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Test negative amount (for corrections)
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        Transaction negativeTx = Transaction.builder()
                .user(boundaryUser)
                .amount(negativeAmount)
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();
        Transaction savedNegativeTx = transactionRepository.save(negativeTx);
        assertThat(savedNegativeTx.getAmount()).isEqualByComparingTo(negativeAmount);
    }
}
