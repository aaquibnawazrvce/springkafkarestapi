package com.example.kafka.service;

import com.example.kafka.exception.RestApiException;
import com.example.kafka.model.ApiRequestDto;
import com.example.kafka.model.ApiResponseDto;
import com.example.kafka.model.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Main service that orchestrates the processing of incoming messages.
 * Coordinates transformation and REST API delivery.
 */
@Slf4j
@Service
public class MessageProcessingService {

    private final TransformationService transformationService;
    private final RestApiClient restApiClient;

    public MessageProcessingService(TransformationService transformationService,
                                   RestApiClient restApiClient) {
        this.transformationService = transformationService;
        this.restApiClient = restApiClient;
    }

    /**
     * Processes an incoming message by transforming it and sending to REST API.
     *
     * @param message The validated incoming message
     * @throws RestApiException if REST API call fails
     */
    public void processMessage(IncomingMessage message) throws RestApiException {
        log.info("Processing message: {}", message.getMessageId());

        // Transform the message
        ApiRequestDto apiRequest = transformationService.transform(message);
        log.debug("Message transformed successfully: {}", message.getMessageId());

        // Send to REST API
        ApiResponseDto response = restApiClient.sendRequest(apiRequest);
        
        log.info("Message processed successfully. MessageId: {}, Response: {}", 
                message.getMessageId(), response.getMessage());
    }
}
