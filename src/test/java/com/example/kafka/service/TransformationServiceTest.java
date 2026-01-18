package com.example.kafka.service;

import com.example.kafka.model.ApiRequestDto;
import com.example.kafka.model.IncomingMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransformationServiceTest {

    @Autowired
    private TransformationService transformationService;

    @Test
    void shouldTransformIncomingMessageToApiRequestDto() {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                .customerName("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .amount(100.50)
                .currency("USD")
                .description("Test transaction")
                .active(true)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-001")
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.of(2025, 12, 24, 10, 30, 0))
                .payload(payload)
                .metadata(new HashMap<>())
                .build();

        // When
        ApiRequestDto result = transformationService.transform(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("MSG-001");
        assertThat(result.getEventName()).isEqualTo("PAYMENT_CREATED");
        assertThat(result.getTimestamp()).isEqualTo("2025-12-24T10:30:00");

        assertThat(result.getCustomer()).isNotNull();
        assertThat(result.getCustomer().getId()).isEqualTo("CUST-123");
        assertThat(result.getCustomer().getFullName()).isEqualTo("John Doe");
        assertThat(result.getCustomer().getContactEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getCustomer().getContactPhone()).isEqualTo("+1234567890");
        assertThat(result.getCustomer().getIsActive()).isTrue();

        assertThat(result.getTransaction()).isNotNull();
        assertThat(result.getTransaction().getAmount()).isEqualTo(100.50);
        assertThat(result.getTransaction().getCurrencyCode()).isEqualTo("USD");
        assertThat(result.getTransaction().getNotes()).isEqualTo("Test transaction");
    }

    @Test
    void shouldHandleNullDescription() {
        // Given
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-456")
                .customerName("Jane Smith")
                .email("jane@example.com")
                .phone("+9876543210")
                .amount(250.75)
                .currency("EUR")
                .description(null)
                .active(false)
                .build();

        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-002")
                .eventType("REFUND_PROCESSED")
                .timestamp(LocalDateTime.of(2025, 12, 24, 14, 45, 30))
                .payload(payload)
                .build();

        // When
        ApiRequestDto result = transformationService.transform(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransaction().getNotes()).isNull();
        assertThat(result.getCustomer().getIsActive()).isFalse();
    }
}
