package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Output DTO for the external REST API.
 * This represents the transformed data structure expected by the target API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequestDto {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("customer")
    private CustomerInfo customer;

    @JsonProperty("transaction")
    private TransactionInfo transaction;

    @JsonProperty("timestamp")
    private String timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {

        @JsonProperty("id")
        private String id;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("contact_email")
        private String contactEmail;

        @JsonProperty("contact_phone")
        private String contactPhone;

        @JsonProperty("is_active")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInfo {

        @JsonProperty("amount")
        private Double amount;

        @JsonProperty("currency_code")
        private String currencyCode;

        @JsonProperty("notes")
        private String notes;
    }
}
