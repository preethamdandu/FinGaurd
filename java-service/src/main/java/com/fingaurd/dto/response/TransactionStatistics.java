package com.fingaurd.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for transaction statistics
 */
@Data
@Builder
public class TransactionStatistics {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal currentBalance;
    private Long totalTransactions;
    private Long incomeTransactions;
    private Long expenseTransactions;
    private Long fraudTransactions;
    private BigDecimal averageIncome;
    private BigDecimal averageExpense;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    
    /**
     * Calculate balance from income and expenses
     */
    public BigDecimal calculateBalance() {
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        return totalIncome.subtract(totalExpenses);
    }
}
