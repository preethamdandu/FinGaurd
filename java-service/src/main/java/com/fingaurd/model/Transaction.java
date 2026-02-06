package com.fingaurd.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction entity representing a financial transaction
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
@org.hibernate.annotations.Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    private String category;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_fraud_flagged")
    @Builder.Default
    private Boolean isFraudFlagged = false;
    
    @Column(name = "fraud_risk_score", precision = 5, scale = 4)
    private BigDecimal fraudRiskScore;
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * Check if transaction is flagged as fraud
     */
    public boolean isFraud() {
        return Boolean.TRUE.equals(isFraudFlagged);
    }
    
    /**
     * Get risk score as double (for calculations)
     */
    public double getRiskScoreAsDouble() {
        return fraudRiskScore != null ? fraudRiskScore.doubleValue() : 0.0;
    }
    
    /**
     * Check if transaction is income
     */
    public boolean isIncome() {
        return TransactionType.INCOME.equals(transactionType);
    }
    
    /**
     * Check if transaction is expense
     */
    public boolean isExpense() {
        return TransactionType.EXPENSE.equals(transactionType);
    }
    
    /**
     * Set timestamps before persisting
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
    
    /**
     * Update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
