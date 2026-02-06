package com.fingaurd.dto.response;

import com.fingaurd.model.Transaction;
import com.fingaurd.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for transaction information
 */
@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String category;
    private String description;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
    private Boolean isFraudFlagged;
    private BigDecimal fraudRiskScore;
    
    /**
     * Factory method to convert from Transaction entity
     */
    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .isFraudFlagged(transaction.isFraud())
                .fraudRiskScore(transaction.getRiskScoreAsDouble() > 0 ? BigDecimal.valueOf(transaction.getRiskScoreAsDouble()) : null)
                .build();
    }
}
