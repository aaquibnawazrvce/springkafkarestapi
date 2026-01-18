package com.example.kafka.service;

import com.example.kafka.exception.ValidationException;
import com.example.kafka.model.IncomingMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ValidationServiceTest {

    @Autowired
    private ValidationService validationService;

    @Test
    void shouldValidateValidMessage() throws ValidationException {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                .customerName("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .amount(100.50)
                .currency("USD")
                .active(true)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-001")
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        // When & Then
        validationService.validate(message); // Should not throw exception
    }

    @Test
    void shouldRejectMessageWithInvalidEmail() {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                .customerName("John Doe")
                .email("invalid-email")
                .phone("+1234567890")
                .amount(100.50)
                .currency("USD")
                .active(true)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-002")
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validate(message))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldRejectMessageWithInvalidAmount() {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                .customerName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .amount(0.0)
                .currency("USD")
                .active(true)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-003")
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validate(message))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void shouldRejectMessageWithInvalidCurrency() {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                .customerName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .amount(100.50)
                .currency("US") // Should be 3 characters
                .active(true)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-004")
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validate(message))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void shouldRejectMessageWithMissingRequiredFields() {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                // Missing customerName
                .email("john@example.com")
                .phone("+1234567890")
                .amount(100.50)
                .currency("USD")
                .active(true)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-005")
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        // When & Then
        assertThatThrownBy(() -> validationService.validate(message))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void shouldValidateJsonString() {
        // Given
        String validJson = """
                {
                    "messageId": "MSG-001",
                    "eventType": "TEST",
                    "timestamp": "2025-12-24T10:30:00",
                    "payload": {
                        "customerId": "CUST-123",
                        "customerName": "John Doe",
                        "email": "john@example.com",
                        "phone": "+1234567890",
                        "amount": 100.50,
                        "currency": "USD",
                        "active": true
                    }
                }
                """;

        // When
        boolean result = validationService.isValidJson(validJson);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectInvalidJsonString() {
        // Given
        String invalidJson = """
                {
                    "messageId": "MSG-001",
                    "payload": {
                        "customerId": "CUST-123"
                    }
                }
                """;

        // When
        boolean result = validationService.isValidJson(invalidJson);

        // Then
        assertThat(result).isFalse();
    }
}
