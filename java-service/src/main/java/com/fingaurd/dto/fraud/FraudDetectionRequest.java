package com.fingaurd.dto.fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionRequest {
    @JsonProperty("user_id")
    private int userId;
    private BigDecimal amount;
    private String timestamp;
}


