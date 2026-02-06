package com.fingaurd.dto.request;

import com.fingaurd.model.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for updating a transaction
 */
@Data
public class TransactionUpdateRequest {
    
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount must not exceed 999,999,999.99")
    private BigDecimal amount;
    
    private TransactionType transactionType;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private LocalDateTime transactionDate;
}

