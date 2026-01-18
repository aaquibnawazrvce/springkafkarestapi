package com.example.kafka.service;

import com.example.kafka.config.ApplicationProperties;
import com.example.kafka.model.DlqMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for handling messages that fail processing
 * and routing them to the Dead Letter Queue (DLQ).
 */
@Slf4j
@Service
public class DeadLetterQueueService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationProperties properties;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueService(KafkaTemplate<String, Object> kafkaTemplate,
                                 ApplicationProperties properties,
                                 ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a failed message to the DLQ topic with error details.
     *
     * @param record The original Kafka record
     * @param exception The exception that caused the failure
     * @param retryCount Number of retry attempts made
     */
    public void sendToDlq(ConsumerRecord<String, ?> record, Exception exception, int retryCount) {
        try {
            String originalMessage = serializeValue(record.value());

            DlqMessage dlqMessage = DlqMessage.builder()
                    .originalMessage(originalMessage)
                    .topic(record.topic())
                    .partition(record.partition())
                    .offset(record.offset())
                    .errorType(exception.getClass().getSimpleName())
                    .errorMessage(exception.getMessage())
                    .stackTrace(getStackTrace(exception))
                    .timestamp(LocalDateTime.now())
                    .retryCount(retryCount)
                    .build();

            String dlqTopic = properties.getKafka().getTopic().getDlq();
            
            log.warn("Sending message to DLQ. Topic: {}, Partition: {}, Offset: {}, Error: {}",
                    record.topic(), record.partition(), record.offset(), exception.getMessage());

            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(dlqTopic, record.key(), dlqMessage);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message to DLQ: {}", ex.getMessage(), ex);
                } else {
                    log.info("Successfully sent message to DLQ. Topic: {}, Partition: {}, Offset: {}",
                            dlqTopic, result.getRecordMetadata().partition(), 
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Critical error: Failed to send message to DLQ", e);
        }
    }

    /**
     * Sends a failed message to DLQ without consumer record context.
     *
     * @param message The message that failed
     * @param exception The exception that caused the failure
     */
    public void sendToDlq(Object message, Exception exception) {
        try {
            String originalMessage = serializeValue(message);

            DlqMessage dlqMessage = DlqMessage.builder()
                    .originalMessage(originalMessage)
                    .topic("unknown")
                    .partition(-1)
                    .offset(-1L)
                    .errorType(exception.getClass().getSimpleName())
                    .errorMessage(exception.getMessage())
                    .stackTrace(getStackTrace(exception))
                    .timestamp(LocalDateTime.now())
                    .retryCount(0)
                    .build();

            String dlqTopic = properties.getKafka().getTopic().getDlq();
            
            log.warn("Sending message to DLQ. Error: {}", exception.getMessage());

            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(dlqTopic, dlqMessage);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message to DLQ: {}", ex.getMessage(), ex);
                } else {
                    log.info("Successfully sent message to DLQ");
                }
            });

        } catch (Exception e) {
            log.error("Critical error: Failed to send message to DLQ", e);
        }
    }

    private String serializeValue(Object value) {
        try {
            if (value instanceof String) {
                return (String) value;
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize value to JSON", e);
            return value.toString();
        }
    }

    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        // Limit stack trace size to prevent message size issues
        if (stackTrace.length() > 5000) {
            return stackTrace.substring(0, 5000) + "... (truncated)";
        }
        return stackTrace;
    }
}
