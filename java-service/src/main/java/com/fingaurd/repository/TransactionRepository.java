package com.fingaurd.repository;

import com.fingaurd.model.Transaction;
import com.fingaurd.model.TransactionType;
import com.fingaurd.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction entity operations
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    /**
     * Find all transactions for a user with pagination
     */
    Page<Transaction> findByUser(User user, Pageable pageable);
    
    /**
     * Find transactions by user and date range
     */
    List<Transaction> findByUserAndTransactionDateBetween(
        User user,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Find transactions by user and date range with pagination
     */
    Page<Transaction> findByUserAndTransactionDateBetween(
        User user,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Find fraud-flagged transactions for a user
     */
    Page<Transaction> findByUserAndIsFraudFlagged(
        User user, 
        Boolean isFraudFlagged, 
        Pageable pageable
    );
    
    /**
     * Find transactions by user and category
     */
    Page<Transaction> findByUserAndCategory(User user, String category, Pageable pageable);
    
    /**
     * Find transactions by user and type
     */
    Page<Transaction> findByUserAndTransactionType(User user, TransactionType type, Pageable pageable);
    
    /**
     * Find transactions by user, type, and category
     */
    Page<Transaction> findByUserAndTransactionTypeAndCategory(User user, TransactionType type, String category, Pageable pageable);
    
    /**
     * Count transactions for a user
     */
    long countByUser(User user);
    
    /**
     * Count transactions by type for a user
     */
    long countByUserAndTransactionType(User user, TransactionType type);
    
    /**
     * Sum amount by user and transaction type
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.transactionType = :type")
    BigDecimal sumAmountByUserAndType(
        @Param("user") User user,
        @Param("type") TransactionType type
    );
    
    /**
     * Sum amount by user, type, and date range
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.transactionType = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserTypeAndDateRange(
        @Param("user") User user,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find recent transactions for a user
     */
    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactionsByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * Find transactions by category for a user with date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.category = :category " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserCategoryAndDateRange(
        @Param("user") User user,
        @Param("category") String category,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Get transaction statistics by category for a user
     */
    @Query("SELECT t.category, COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.user = :user AND t.transactionType = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> getTransactionStatsByCategory(
        @Param("user") User user,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
