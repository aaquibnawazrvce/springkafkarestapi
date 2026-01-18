package com.example.kafka.service;

import com.example.kafka.model.ApiRequestDto;
import com.example.kafka.model.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Service responsible for transforming incoming Kafka messages
 * to the format expected by the external REST API.
 */
@Slf4j
@Service
public class TransformationService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Transforms an IncomingMessage to ApiRequestDto.
     *
     * @param incomingMessage The message from Kafka
     * @return Transformed DTO ready for REST API
     */
    public ApiRequestDto transform(IncomingMessage incomingMessage) {
        log.debug("Transforming message: {}", incomingMessage.getMessageId());

        ApiRequestDto.CustomerInfo customer = ApiRequestDto.CustomerInfo.builder()
                .id(incomingMessage.getPayload().getCustomerId())
                .fullName(incomingMessage.getPayload().getCustomerName())
                .contactEmail(incomingMessage.getPayload().getEmail())
                .contactPhone(incomingMessage.getPayload().getPhone())
                .isActive(incomingMessage.getPayload().getActive())
                .build();

        ApiRequestDto.TransactionInfo transaction = ApiRequestDto.TransactionInfo.builder()
                .amount(incomingMessage.getPayload().getAmount())
                .currencyCode(incomingMessage.getPayload().getCurrency())
                .notes(incomingMessage.getPayload().getDescription())
                .build();

        ApiRequestDto apiRequest = ApiRequestDto.builder()
                .transactionId(incomingMessage.getMessageId())
                .eventName(incomingMessage.getEventType())
                .customer(customer)
                .transaction(transaction)
                .timestamp(incomingMessage.getTimestamp().format(ISO_FORMATTER))
                .build();

        log.debug("Transformation complete for message: {}", incomingMessage.getMessageId());
        return apiRequest;
    }
}
