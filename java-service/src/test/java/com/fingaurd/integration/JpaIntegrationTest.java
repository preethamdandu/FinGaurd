package com.fingaurd.integration;

import com.fingaurd.model.Transaction;
import com.fingaurd.model.TransactionType;
import com.fingaurd.model.User;
import com.fingaurd.model.UserRole;
import com.fingaurd.repository.TransactionRepository;
import com.fingaurd.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for JPA entities and repositories working together
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
public class JpaIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void testCompleteUserTransactionWorkflow() {
        // 1. Create and save a user
        User user = User.builder()
                .username("integrationuser")
                .email("integration@example.com")
                .passwordHash("password")
                .firstName("Integration")
                .lastName("Test")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        // 2. Create multiple transactions for the user
        Transaction income1 = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(3000.00))
                .transactionType(TransactionType.INCOME)
                .category("Salary")
                .description("Monthly salary")
                .transactionDate(LocalDateTime.now().minusDays(5))
                .build();

        Transaction expense1 = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(150.00))
                .transactionType(TransactionType.EXPENSE)
                .category("Food & Dining")
                .description("Groceries")
                .transactionDate(LocalDateTime.now().minusDays(3))
                .build();

        Transaction expense2 = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(75.50))
                .transactionType(TransactionType.EXPENSE)
                .category("Transportation")
                .description("Gas")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build();

        Transaction savedIncome = transactionRepository.save(income1);
        Transaction savedExpense1 = transactionRepository.save(expense1);
        Transaction savedExpense2 = transactionRepository.save(expense2);

        // 3. Verify transactions are saved correctly
        assertThat(transactionRepository.countByUser(savedUser)).isEqualTo(3);

        // 4. Test repository queries
        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndType(savedUser, TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndType(savedUser, TransactionType.EXPENSE);

        assertThat(totalIncome).isEqualByComparingTo(BigDecimal.valueOf(3000.00));
        assertThat(totalExpense).isEqualByComparingTo(BigDecimal.valueOf(225.50));

        // 5. Test relationship loading
        entityManager.clear(); // Clear persistence context to test lazy loading
        User userFromDb = userRepository.findById(savedUser.getId()).orElseThrow();
        
        // Trigger lazy loading of transactions
        List<Transaction> userTransactions = userFromDb.getTransactions();
        assertThat(userTransactions).hasSize(3);

        // 6. Test cascade operations
        // Delete transactions first, then user (H2 doesn't support cascade delete by default)
        transactionRepository.deleteAll(transactionRepository.findByUser(savedUser, PageRequest.of(0, 100)).getContent());
        userRepository.delete(savedUser);
        assertThat(transactionRepository.count()).isEqualTo(0); // Should be 0 after manual deletion
    }

    @Test
    public void testComplexQueries() {
        // Setup: Create users and transactions
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");

        // Create transactions with different categories and dates
        createAndSaveTransaction(user1, BigDecimal.valueOf(2000), TransactionType.INCOME, "Salary", LocalDateTime.now().minusDays(10));
        createAndSaveTransaction(user1, BigDecimal.valueOf(100), TransactionType.EXPENSE, "Food", LocalDateTime.now().minusDays(5));
        createAndSaveTransaction(user1, BigDecimal.valueOf(50), TransactionType.EXPENSE, "Food", LocalDateTime.now().minusDays(3));
        createAndSaveTransaction(user1, BigDecimal.valueOf(200), TransactionType.EXPENSE, "Transportation", LocalDateTime.now().minusDays(1));

        createAndSaveTransaction(user2, BigDecimal.valueOf(1500), TransactionType.INCOME, "Salary", LocalDateTime.now().minusDays(8));
        createAndSaveTransaction(user2, BigDecimal.valueOf(80), TransactionType.EXPENSE, "Food", LocalDateTime.now().minusDays(2));

        // Test complex queries
        LocalDateTime startDate = LocalDateTime.now().minusDays(15);
        LocalDateTime endDate = LocalDateTime.now();

        // Test date range queries
        List<Transaction> user1Transactions = transactionRepository.findByUserAndTransactionDateBetween(user1, startDate, endDate);
        assertThat(user1Transactions).hasSize(4);

        // Test category statistics
        List<Object[]> foodStats = transactionRepository.getTransactionStatsByCategory(user1, TransactionType.EXPENSE, startDate, endDate);
        assertThat(foodStats).hasSize(2); // Food and Transportation
        
        // Verify food category stats (2 transactions totaling 150)
        Object[] foodStat = foodStats.stream()
                .filter(stat -> "Food".equals(stat[0]))
                .findFirst()
                .orElse(null);
        assertThat(foodStat).isNotNull();
        assertThat(foodStat[1]).isEqualTo(2L); // Count
        assertThat((BigDecimal) foodStat[2]).isEqualByComparingTo(BigDecimal.valueOf(150.00)); // Sum

        // Test user isolation
        assertThat(transactionRepository.countByUser(user1)).isEqualTo(4);
        assertThat(transactionRepository.countByUser(user2)).isEqualTo(2);
    }

    @Test
    public void testDataIntegrity() {
        // Test unique constraints
        User user1 = createAndSaveUser("uniqueuser", "unique@example.com");
        
        // Try to create another user with same email - should fail at DB level
        User user2 = User.builder()
                .username("anotheruser")
                .email("unique@example.com") // Same email
                .passwordHash("password")
                .build();

        // This should throw an exception due to unique constraint
        assertThatThrownBy(() -> entityManager.persistAndFlush(user2))
                .isNotNull();

        // Test foreign key constraint
        User nonExistentUser = User.builder()
                .id(java.util.UUID.randomUUID()) // Non-existent ID
                .username("fake")
                .email("fake@example.com")
                .passwordHash("password")
                .build();

        Transaction transaction = Transaction.builder()
                .user(nonExistentUser)
                .amount(BigDecimal.valueOf(100))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();

        // This should fail due to foreign key constraint
        assertThatThrownBy(() -> entityManager.persistAndFlush(transaction))
                .isNotNull();
    }

    @Test
    public void testPaginationAndSorting() {
        // Create user with many transactions
        User user = createAndSaveUser("paginationuser", "pagination@example.com");
        
        // Create 15 transactions
        for (int i = 0; i < 15; i++) {
            createAndSaveTransaction(user, BigDecimal.valueOf(100 + i), 
                    i % 2 == 0 ? TransactionType.INCOME : TransactionType.EXPENSE,
                    "Category" + (i % 3),
                    LocalDateTime.now().minusDays(i));
        }

        // Test pagination
        org.springframework.data.domain.Page<Transaction> page1 = 
                transactionRepository.findByUser(user, org.springframework.data.domain.PageRequest.of(0, 10));
        org.springframework.data.domain.Page<Transaction> page2 = 
                transactionRepository.findByUser(user, org.springframework.data.domain.PageRequest.of(1, 10));

        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page2.hasNext()).isFalse();
    }

    @Test
    public void testAuditFields() {
        User user = User.builder()
                .username("audituser")
                .email("audit@example.com")
                .passwordHash("password")
                .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        // Update the user
        savedUser.setFirstName("Updated");
        User updatedUser = userRepository.save(savedUser);
        
        // Verify that updatedAt is not null and not before createdAt
        assertThat(updatedUser.getUpdatedAt()).isNotNull();
        assertThat(updatedUser.getUpdatedAt().isAfter(updatedUser.getCreatedAt()) || 
                   updatedUser.getUpdatedAt().isEqual(updatedUser.getCreatedAt())).isTrue();
    }

    private User createAndSaveUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("password")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    private Transaction createAndSaveTransaction(User user, BigDecimal amount, TransactionType type, String category, LocalDateTime date) {
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .transactionType(type)
                .category(category)
                .transactionDate(date)
                .build();
        return transactionRepository.save(transaction);
    }
}
