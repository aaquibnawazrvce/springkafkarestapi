package com.example.kafka.service;

import com.example.kafka.config.ApplicationProperties;
import com.example.kafka.exception.ValidationException;
import com.example.kafka.model.IncomingMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for validating incoming messages using both JSON Schema
 * and Bean Validation annotations.
 */
@Slf4j
@Service
public class ValidationService {

    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties properties;
    private final ResourceLoader resourceLoader;
    private JsonSchema jsonSchema;

    public ValidationService(Validator validator,
                           ObjectMapper objectMapper,
                           ApplicationProperties properties,
                           ResourceLoader resourceLoader) {
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource(properties.getValidation().getJsonSchemaPath());
            try (InputStream inputStream = resource.getInputStream()) {
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                JsonNode schemaNode = objectMapper.readTree(inputStream);
                this.jsonSchema = factory.getSchema(schemaNode);
                log.info("JSON Schema loaded successfully from: {}", properties.getValidation().getJsonSchemaPath());
            }
        } catch (Exception e) {
            log.error("Failed to load JSON schema", e);
            throw new RuntimeException("Failed to initialize JSON schema validator", e);
        }
    }

    /**
     * Validates an incoming message using both JSON Schema and Bean Validation.
     *
     * @param message The message to validate
     * @throws ValidationException if validation fails
     */
    public void validate(IncomingMessage message) throws ValidationException {
        log.debug("Validating message with ID: {}", message.getMessageId());

        // JSON Schema validation
        validateJsonSchema(message);

        // Bean validation
        validateBean(message);

        log.debug("Message validation successful for ID: {}", message.getMessageId());
    }

    private void validateJsonSchema(IncomingMessage message) throws ValidationException {
        try {
            JsonNode messageNode = objectMapper.valueToTree(message);
            Set<ValidationMessage> errors = jsonSchema.validate(messageNode);

            if (!errors.isEmpty()) {
                String errorMessage = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining(", "));
                
                log.warn("JSON Schema validation failed for message {}: {}", 
                        message.getMessageId(), errorMessage);
                throw new ValidationException("JSON Schema validation failed: " + errorMessage);
            }
        } catch (Exception e) {
            if (e instanceof ValidationException) {
                throw (ValidationException) e;
            }
            log.error("Error during JSON schema validation", e);
            throw new ValidationException("JSON Schema validation error: " + e.getMessage());
        }
    }

    private void validateBean(IncomingMessage message) throws ValidationException {
        Set<ConstraintViolation<IncomingMessage>> violations = validator.validate(message);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Bean validation failed for message {}: {}", 
                    message.getMessageId(), errorMessage);
            throw new ValidationException("Bean validation failed: " + errorMessage);
        }
    }

    /**
     * Validates raw JSON string before deserialization.
     *
     * @param jsonString The JSON string to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidJson(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);
            return errors.isEmpty();
        } catch (Exception e) {
            log.debug("Invalid JSON: {}", e.getMessage());
            return false;
        }
    }
}
