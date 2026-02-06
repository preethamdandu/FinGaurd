package com.fingaurd.repository;

import com.fingaurd.model.Transaction;
import com.fingaurd.model.TransactionType;
import com.fingaurd.model.User;
import com.fingaurd.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for TransactionRepository
 */
@DataJpaTest
@ActiveProfiles("test")
public class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User anotherUser;
    private Transaction incomeTransaction;
    private Transaction expenseTransaction;
    private Transaction fraudTransaction;

    @BeforeEach
    public void setUp() {
        // Create test users
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();

        anotherUser = User.builder()
                .username("anotheruser")
                .email("another@example.com")
                .passwordHash("password")
                .firstName("Another")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(anotherUser);

        // Create test transactions
        incomeTransaction = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(2000.00))
                .transactionType(TransactionType.INCOME)
                .category("Salary")
                .description("Monthly salary")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .isFraudFlagged(false)
                .fraudRiskScore(BigDecimal.valueOf(0.1))
                .build();

        expenseTransaction = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(100.50))
                .transactionType(TransactionType.EXPENSE)
                .category("Food & Dining")
                .description("Lunch at restaurant")
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(false)
                .fraudRiskScore(BigDecimal.valueOf(0.2))
                .build();

        fraudTransaction = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(10000.00))
                .transactionType(TransactionType.EXPENSE)
                .category("Other")
                .description("Suspicious large transaction")
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(true)
                .fraudRiskScore(BigDecimal.valueOf(0.95))
                .build();

        entityManager.persistAndFlush(incomeTransaction);
        entityManager.persistAndFlush(expenseTransaction);
        entityManager.persistAndFlush(fraudTransaction);
    }

    @Test
    public void testFindByUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> transactions = transactionRepository.findByUser(testUser, pageable);

        // Then
        assertThat(transactions.getContent()).hasSize(3);
        assertThat(transactions.getTotalElements()).isEqualTo(3);
        assertThat(transactions.getTotalPages()).isEqualTo(1);
    }

    @Test
    public void testFindByUserWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Transaction> transactions = transactionRepository.findByUser(testUser, pageable);

        // Then
        assertThat(transactions.getContent()).hasSize(2);
        assertThat(transactions.getTotalElements()).isEqualTo(3);
        assertThat(transactions.getTotalPages()).isEqualTo(2);
        assertThat(transactions.hasNext()).isTrue();
    }

    @Test
    public void testFindByUserAndTransactionDateBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When
        List<Transaction> transactions = transactionRepository.findByUserAndTransactionDateBetween(
                testUser, startDate, endDate);

        // Then
        assertThat(transactions).hasSize(3);
    }

    @Test
    public void testFindByUserAndTransactionDateBetweenNoResults() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().minusDays(5);

        // When
        List<Transaction> transactions = transactionRepository.findByUserAndTransactionDateBetween(
                testUser, startDate, endDate);

        // Then
        assertThat(transactions).isEmpty();
    }

    @Test
    public void testFindByUserAndIsFraudFlagged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> fraudTransactions = transactionRepository.findByUserAndIsFraudFlagged(
                testUser, true, pageable);
        Page<Transaction> normalTransactions = transactionRepository.findByUserAndIsFraudFlagged(
                testUser, false, pageable);

        // Then
        assertThat(fraudTransactions.getContent()).hasSize(1);
        assertThat(fraudTransactions.getContent().get(0)).isEqualTo(fraudTransaction);
        assertThat(normalTransactions.getContent()).hasSize(2);
    }

    @Test
    public void testFindByUserAndCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> foodTransactions = transactionRepository.findByUserAndCategory(
                testUser, "Food & Dining", pageable);
        Page<Transaction> salaryTransactions = transactionRepository.findByUserAndCategory(
                testUser, "Salary", pageable);

        // Then
        assertThat(foodTransactions.getContent()).hasSize(1);
        assertThat(foodTransactions.getContent().get(0)).isEqualTo(expenseTransaction);
        assertThat(salaryTransactions.getContent()).hasSize(1);
        assertThat(salaryTransactions.getContent().get(0)).isEqualTo(incomeTransaction);
    }

    @Test
    public void testFindByUserAndTransactionType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> incomeTransactions = transactionRepository.findByUserAndTransactionType(
                testUser, TransactionType.INCOME, pageable);
        Page<Transaction> expenseTransactions = transactionRepository.findByUserAndTransactionType(
                testUser, TransactionType.EXPENSE, pageable);

        // Then
        assertThat(incomeTransactions.getContent()).hasSize(1);
        assertThat(incomeTransactions.getContent().get(0)).isEqualTo(incomeTransaction);
        assertThat(expenseTransactions.getContent()).hasSize(2);
    }

    @Test
    public void testCountByUser() {
        // When
        long count = transactionRepository.countByUser(testUser);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    public void testCountByUserAndTransactionType() {
        // When
        long incomeCount = transactionRepository.countByUserAndTransactionType(testUser, TransactionType.INCOME);
        long expenseCount = transactionRepository.countByUserAndTransactionType(testUser, TransactionType.EXPENSE);

        // Then
        assertThat(incomeCount).isEqualTo(1);
        assertThat(expenseCount).isEqualTo(2);
    }

    @Test
    public void testSumAmountByUserAndType() {
        // When
        BigDecimal incomeSum = transactionRepository.sumAmountByUserAndType(testUser, TransactionType.INCOME);
        BigDecimal expenseSum = transactionRepository.sumAmountByUserAndType(testUser, TransactionType.EXPENSE);

        // Then
        assertThat(incomeSum).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
        assertThat(expenseSum).isEqualByComparingTo(BigDecimal.valueOf(10100.50));
    }

    @Test
    public void testSumAmountByUserTypeAndDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When
        BigDecimal incomeSum = transactionRepository.sumAmountByUserTypeAndDateRange(
                testUser, TransactionType.INCOME, startDate, endDate);
        BigDecimal expenseSum = transactionRepository.sumAmountByUserTypeAndDateRange(
                testUser, TransactionType.EXPENSE, startDate, endDate);

        // Then
        assertThat(incomeSum).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
        assertThat(expenseSum).isEqualByComparingTo(BigDecimal.valueOf(10100.50));
    }

    @Test
    public void testFindRecentTransactionsByUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Transaction> recentTransactions = transactionRepository.findRecentTransactionsByUser(testUser, pageable);

        // Then
        assertThat(recentTransactions).hasSize(3);
        // Should be ordered by transaction date descending (most recent first)
        assertThat(recentTransactions.get(0).getTransactionDate())
                .isAfterOrEqualTo(recentTransactions.get(1).getTransactionDate());
        assertThat(recentTransactions.get(1).getTransactionDate())
                .isAfterOrEqualTo(recentTransactions.get(2).getTransactionDate());
    }

    @Test
    public void testFindByUserCategoryAndDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> foodTransactions = transactionRepository.findByUserCategoryAndDateRange(
                testUser, "Food & Dining", startDate, endDate, pageable);

        // Then
        assertThat(foodTransactions.getContent()).hasSize(1);
        assertThat(foodTransactions.getContent().get(0)).isEqualTo(expenseTransaction);
    }

    @Test
    public void testGetTransactionStatsByCategory() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When
        List<Object[]> stats = transactionRepository.getTransactionStatsByCategory(
                testUser, TransactionType.EXPENSE, startDate, endDate);

        // Then
        assertThat(stats).hasSize(2); // Food & Dining and Other
        // Stats should be ordered by sum amount descending
        assertThat(stats.get(0)[0]).isEqualTo("Other"); // 10000.00
        assertThat(stats.get(1)[0]).isEqualTo("Food & Dining"); // 100.50
    }

    @Test
    public void testSaveTransaction() {
        // Given
        Transaction newTransaction = Transaction.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(500.00))
                .transactionType(TransactionType.EXPENSE)
                .category("Shopping")
                .description("New transaction")
                .transactionDate(LocalDateTime.now())
                .build();

        // When
        Transaction saved = transactionRepository.save(newTransaction);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualTo(BigDecimal.valueOf(500.00));
        assertThat(transactionRepository.countByUser(testUser)).isEqualTo(4);
    }

    @Test
    public void testUpdateTransaction() {
        // Given
        Transaction transactionToUpdate = transactionRepository.findByUser(testUser, PageRequest.of(0, 1))
                .getContent().get(0);
        transactionToUpdate.setDescription("Updated description");

        // When
        Transaction updated = transactionRepository.save(transactionToUpdate);

        // Then
        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }

    @Test
    public void testDeleteTransaction() {
        // Given
        UUID transactionId = incomeTransaction.getId();

        // When
        transactionRepository.delete(incomeTransaction);

        // Then
        assertThat(transactionRepository.findById(transactionId)).isEmpty();
        assertThat(transactionRepository.countByUser(testUser)).isEqualTo(2);
    }

    @Test
    public void testFindById() {
        // When
        Transaction found = transactionRepository.findById(incomeTransaction.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found).isEqualTo(incomeTransaction);
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        Transaction found = transactionRepository.findById(UUID.randomUUID()).orElse(null);

        // Then
        assertThat(found).isNull();
    }

    @Test
    public void testFindAllTransactions() {
        // When
        Iterable<Transaction> allTransactions = transactionRepository.findAll();

        // Then
        assertThat(allTransactions).hasSize(3);
    }

    @Test
    public void testCountTransactions() {
        // When
        long count = transactionRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    public void testTransactionsForDifferentUsers() {
        // Given
        Transaction anotherUserTransaction = Transaction.builder()
                .user(anotherUser)
                .amount(BigDecimal.valueOf(300.00))
                .transactionType(TransactionType.EXPENSE)
                .category("Transportation")
                .description("Another user's transaction")
                .transactionDate(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(anotherUserTransaction);

        // When
        long testUserCount = transactionRepository.countByUser(testUser);
        long anotherUserCount = transactionRepository.countByUser(anotherUser);

        // Then
        assertThat(testUserCount).isEqualTo(3);
        assertThat(anotherUserCount).isEqualTo(1);
    }
}
