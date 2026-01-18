package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from the external REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("status_code")
    private Integer statusCode;
}
