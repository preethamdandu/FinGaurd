package com.fingaurd.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for transaction summary information
 */
@Data
@Builder
public class TransactionSummary {
    private int periodDays;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netAmount;
    private long transactionCount;
    private long fraudCount;
    private List<String> topCategories;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}

