package com.example.kafka.integration;

import com.example.kafka.config.ApplicationProperties;
import com.example.kafka.model.IncomingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"test-input-topic", "test-dlq-topic"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ApplicationProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private KafkaMessageListenerContainer<String, Object> dlqContainer;
    private BlockingQueue<ConsumerRecord<String, Object>> dlqRecords;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();
        configureFor("localhost", 8081);

        // Setup DLQ consumer
        dlqRecords = new LinkedBlockingQueue<>();
        ContainerProperties containerProperties = new ContainerProperties("test-dlq-topic");
        containerProperties.setMessageListener((MessageListener<String, Object>) dlqRecords::add);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlq-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, Object> consumerFactory = 
                new DefaultKafkaConsumerFactory<>(consumerProps);
        
        dlqContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        dlqContainer.start();
        ContainerTestUtils.waitForAssignment(dlqContainer, 1);
    }

    @AfterEach
    void tearDown() {
        if (dlqContainer != null) {
            dlqContainer.stop();
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void shouldProcessValidMessageSuccessfully() throws Exception {
        // Given
        stubFor(post(urlEqualTo("/api/v1/process"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "message": "Processed successfully",
                                    "transaction_id": "MSG-001",
                                    "status_code": 200
                                }
                                """)));

        IncomingMessage message = createValidMessage("MSG-001");

        // When
        kafkaTemplate.send("test-input-topic", message).get();

        // Then
        // Wait for processing
        Thread.sleep(3000);

        // Verify REST API was called
        verify(postRequestedFor(urlEqualTo("/api/v1/process")));

        // Verify no message in DLQ
        ConsumerRecord<String, Object> dlqRecord = dlqRecords.poll(2, TimeUnit.SECONDS);
        assertThat(dlqRecord).isNull();
    }

    @Test
    void shouldSendInvalidMessageToDlq() throws Exception {
        // Given - Invalid message (missing required fields)
        IncomingMessage message = IncomingMessage.builder()
                .messageId("MSG-INVALID")
                .eventType("TEST_EVENT")
                .timestamp(LocalDateTime.now())
                .payload(IncomingMessage.PayloadData.builder()
                        .customerId("CUST-999")
                        // Missing required fields
                        .build())
                .build();

        // When
        kafkaTemplate.send("test-input-topic", message).get();

        // Then
        // Wait for message to be sent to DLQ
        ConsumerRecord<String, Object> dlqRecord = dlqRecords.poll(5, TimeUnit.SECONDS);
        assertThat(dlqRecord).isNotNull();
        assertThat(dlqRecord.topic()).isEqualTo("test-dlq-topic");
    }

    @Test
    void shouldSendMessageToDlqWhenRestApiUnavailable() throws Exception {
        // Given
        stubFor(post(urlEqualTo("/api/v1/process"))
                .willReturn(aResponse().withStatus(503)));

        IncomingMessage message = createValidMessage("MSG-002");

        // When
        kafkaTemplate.send("test-input-topic", message).get();

        // Then
        // Wait for retries and DLQ routing
        ConsumerRecord<String, Object> dlqRecord = dlqRecords.poll(10, TimeUnit.SECONDS);
        assertThat(dlqRecord).isNotNull();
        assertThat(dlqRecord.topic()).isEqualTo("test-dlq-topic");

        // Verify multiple retry attempts were made
        verify(moreThanOrExactly(2), postRequestedFor(urlEqualTo("/api/v1/process")));
    }

    private IncomingMessage createValidMessage(String messageId) {
        IncomingMessage.PayloadData payload = IncomingMessage.PayloadData.builder()
                .customerId("CUST-123")
                .customerName("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .amount(100.50)
                .currency("USD")
                .description("Integration test transaction")
                .active(true)
                .build();

        return IncomingMessage.builder()
                .messageId(messageId)
                .eventType("PAYMENT_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .metadata(new HashMap<>())
                .build();
    }
}
