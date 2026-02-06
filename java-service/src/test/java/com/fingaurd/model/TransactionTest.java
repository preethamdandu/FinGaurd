package com.fingaurd.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Transaction entity
 */
@DataJpaTest
@ActiveProfiles("test")
public class TransactionTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testTransactionCreation() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction transaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(150.50))
                .transactionType(TransactionType.EXPENSE)
                .category("Food & Dining")
                .description("Lunch at restaurant")
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(false)
                .fraudRiskScore(BigDecimal.valueOf(0.1))
                .build();

        // When
        Transaction savedTransaction = entityManager.persistAndFlush(transaction);

        // Then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getUser()).isEqualTo(savedUser);
        assertThat(savedTransaction.getAmount()).isEqualTo(BigDecimal.valueOf(150.50));
        assertThat(savedTransaction.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(savedTransaction.getCategory()).isEqualTo("Food & Dining");
        assertThat(savedTransaction.getDescription()).isEqualTo("Lunch at restaurant");
        assertThat(savedTransaction.getTransactionDate()).isNotNull();
        assertThat(savedTransaction.getCreatedAt()).isNotNull();
        assertThat(savedTransaction.getUpdatedAt()).isNotNull();
        assertThat(savedTransaction.getIsFraudFlagged()).isFalse();
        assertThat(savedTransaction.getFraudRiskScore()).isEqualTo(BigDecimal.valueOf(0.1));
    }

    @Test
    public void testTransactionWithDefaultValues() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction transaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.INCOME)
                .transactionDate(LocalDateTime.now())
                .build();

        // When
        Transaction savedTransaction = entityManager.persistAndFlush(transaction);

        // Then
        assertThat(savedTransaction.getIsFraudFlagged()).isFalse(); // Default value
        assertThat(savedTransaction.getFraudRiskScore()).isNull();
    }

    @Test
    public void testTransactionTypeEnum() {
        // Test all transaction type values
        assertThat(TransactionType.INCOME).isNotNull();
        assertThat(TransactionType.EXPENSE).isNotNull();
        
        // Test type values
        assertThat(TransactionType.INCOME.toString()).isEqualTo("INCOME");
        assertThat(TransactionType.EXPENSE.toString()).isEqualTo("EXPENSE");
    }

    @Test
    public void testIsFraud() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction fraudTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(1000.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(true)
                .build();

        Transaction normalTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(50.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .isFraudFlagged(false)
                .build();

        // When
        Transaction savedFraud = entityManager.persistAndFlush(fraudTransaction);
        Transaction savedNormal = entityManager.persistAndFlush(normalTransaction);

        // Then
        assertThat(savedFraud.isFraud()).isTrue();
        assertThat(savedNormal.isFraud()).isFalse();
    }

    @Test
    public void testGetRiskScoreAsDouble() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction transactionWithRisk = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(500.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .fraudRiskScore(BigDecimal.valueOf(0.75))
                .build();

        Transaction transactionWithoutRisk = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .fraudRiskScore(null)
                .build();

        // When
        Transaction savedWithRisk = entityManager.persistAndFlush(transactionWithRisk);
        Transaction savedWithoutRisk = entityManager.persistAndFlush(transactionWithoutRisk);

        // Then
        assertThat(savedWithRisk.getRiskScoreAsDouble()).isEqualTo(0.75);
        assertThat(savedWithoutRisk.getRiskScoreAsDouble()).isEqualTo(0.0);
    }

    @Test
    public void testIsIncome() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction incomeTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(2000.00))
                .transactionType(TransactionType.INCOME)
                .transactionDate(LocalDateTime.now())
                .build();

        Transaction expenseTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();

        // When
        Transaction savedIncome = entityManager.persistAndFlush(incomeTransaction);
        Transaction savedExpense = entityManager.persistAndFlush(expenseTransaction);

        // Then
        assertThat(savedIncome.isIncome()).isTrue();
        assertThat(savedExpense.isIncome()).isFalse();
    }

    @Test
    public void testIsExpense() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction incomeTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(2000.00))
                .transactionType(TransactionType.INCOME)
                .transactionDate(LocalDateTime.now())
                .build();

        Transaction expenseTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();

        // When
        Transaction savedIncome = entityManager.persistAndFlush(incomeTransaction);
        Transaction savedExpense = entityManager.persistAndFlush(expenseTransaction);

        // Then
        assertThat(savedIncome.isExpense()).isFalse();
        assertThat(savedExpense.isExpense()).isTrue();
    }

    @Test
    public void testPrePersistCallback() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction transaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();

        // When
        Transaction savedTransaction = entityManager.persistAndFlush(transaction);

        // Then
        assertThat(savedTransaction.getCreatedAt()).isNotNull();
        assertThat(savedTransaction.getUpdatedAt()).isNotNull();
        assertThat(savedTransaction.getCreatedAt()).isEqualTo(savedTransaction.getUpdatedAt());
    }

    @Test
    public void testPreUpdateCallback() throws InterruptedException {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction transaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();

        Transaction savedTransaction = entityManager.persistAndFlush(transaction);
        LocalDateTime originalUpdatedAt = savedTransaction.getUpdatedAt();

        // When
        Thread.sleep(100); // Ensure time difference
        savedTransaction.setDescription("Updated description");
        Transaction updatedTransaction = entityManager.persistAndFlush(savedTransaction);

        // Then
        assertThat(updatedTransaction.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedTransaction.getDescription()).isEqualTo("Updated description");
    }

    @Test
    public void testTransactionWithLargeAmount() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        Transaction largeTransaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(999999999.99))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .build();

        // When
        Transaction savedTransaction = entityManager.persistAndFlush(largeTransaction);

        // Then
        assertThat(savedTransaction.getAmount()).isEqualTo(BigDecimal.valueOf(999999999.99));
    }

    @Test
    public void testTransactionWithLongDescription() {
        // Given
        User user = createTestUser();
        User savedUser = entityManager.persistAndFlush(user);

        String longDescription = "This is a very long description for testing purposes. " +
                "It should contain enough text to test the TEXT column type in the database. " +
                "This description is intentionally long to ensure that the database can handle " +
                "large text fields properly without any truncation or data loss issues.";

        Transaction transaction = Transaction.builder()
                .user(savedUser)
                .amount(BigDecimal.valueOf(100.00))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .description(longDescription)
                .build();

        // When
        Transaction savedTransaction = entityManager.persistAndFlush(transaction);

        // Then
        assertThat(savedTransaction.getDescription()).isEqualTo(longDescription);
    }

    private User createTestUser() {
        return User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .isActive(true)
                .isVerified(false)
                .build();
    }
}
