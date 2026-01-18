package com.example.kafka.consumer;

import com.example.kafka.config.ApplicationProperties;
import com.example.kafka.exception.ValidationException;
import com.example.kafka.model.IncomingMessage;
import com.example.kafka.service.DeadLetterQueueService;
import com.example.kafka.service.MessageProcessingService;
import com.example.kafka.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to incoming messages, validates them,
 * transforms them, and forwards to REST API with error handling.
 */
@Slf4j
@Component
public class MessageConsumer {

    private final ValidationService validationService;
    private final MessageProcessingService processingService;
    private final DeadLetterQueueService dlqService;
    private final ApplicationProperties properties;

    public MessageConsumer(ValidationService validationService,
                          MessageProcessingService processingService,
                          DeadLetterQueueService dlqService,
                          ApplicationProperties properties) {
        this.validationService = validationService;
        this.processingService = processingService;
        this.dlqService = dlqService;
        this.properties = properties;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.input}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload IncomingMessage message,
                       ConsumerRecord<String, IncomingMessage> record,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       Acknowledgment acknowledgment) {

        log.info("Received message from topic: {}, partition: {}, offset: {}, messageId: {}",
                topic, partition, offset, message != null ? message.getMessageId() : "null");

        try {
            // Step 1: Validate the message
            if (message == null) {
                throw new ValidationException("Received null message");
            }

            validationService.validate(message);
            log.debug("Message validation successful for messageId: {}", message.getMessageId());

            // Step 2: Process (transform and send to REST API)
            processingService.processMessage(message);
            log.info("Message processed successfully. MessageId: {}", message.getMessageId());

            // Step 3: Commit offset
            acknowledgment.acknowledge();
            log.debug("Message acknowledged. Offset: {}", offset);

        } catch (ValidationException e) {
            log.error("Validation failed for message at offset {}: {}", offset, e.getMessage());
            handleFailure(record, e, acknowledgment);

        } catch (Exception e) {
            log.error("Error processing message at offset {}: {}", offset, e.getMessage(), e);
            handleFailure(record, e, acknowledgment);
        }
    }

    private void handleFailure(ConsumerRecord<String, IncomingMessage> record,
                              Exception exception,
                              Acknowledgment acknowledgment) {
        try {
            // Send to DLQ
            dlqService.sendToDlq(record, exception, 0);

            // Acknowledge the message to prevent reprocessing
            acknowledgment.acknowledge();
            log.info("Message sent to DLQ and acknowledged. Topic: {}, Partition: {}, Offset: {}",
                    record.topic(), record.partition(), record.offset());

        } catch (Exception e) {
            log.error("Critical error handling failure for offset {}: {}",
                    record.offset(), e.getMessage(), e);
            // Still acknowledge to prevent blocking
            acknowledgment.acknowledge();
        }
    }
}
