package com.fingaurd.dto.fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FraudDetectionResponse {
    @JsonProperty("is_fraudulent")
    private boolean isFraudulent;
    private String reason;
    @JsonProperty("risk_score")
    private double riskScore;
}


