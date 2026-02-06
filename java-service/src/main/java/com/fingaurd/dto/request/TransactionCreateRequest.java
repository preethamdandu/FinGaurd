package com.fingaurd.dto.request;

import com.fingaurd.model.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount must not exceed 999,999,999.99")
    private BigDecimal amount;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private LocalDateTime transactionDate = LocalDateTime.now();
}
