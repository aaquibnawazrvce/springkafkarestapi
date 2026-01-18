package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Incoming message from Kafka topic.
 * This represents the raw message structure expected from the Kafka producer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMessage {

    @NotBlank(message = "Message ID is required")
    @JsonProperty("messageId")
    private String messageId;

    @NotBlank(message = "Event type is required")
    @JsonProperty("eventType")
    private String eventType;

    @NotNull(message = "Timestamp is required")
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @NotNull(message = "Payload is required")
    @JsonProperty("payload")
    private PayloadData payload;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayloadData {

        @NotBlank(message = "Customer ID is required")
        @JsonProperty("customerId")
        private String customerId;

        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
        @JsonProperty("customerName")
        private String customerName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @JsonProperty("email")
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be a valid E.164 format")
        @JsonProperty("phone")
        private String phone;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @JsonProperty("amount")
        private Double amount;

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        @JsonProperty("currency")
        private String currency;

        @JsonProperty("description")
        private String description;

        @NotNull(message = "Active status is required")
        @JsonProperty("active")
        private Boolean active;
    }
}
