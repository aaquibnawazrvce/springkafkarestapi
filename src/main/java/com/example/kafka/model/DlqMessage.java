package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dead Letter Queue message wrapper.
 * Contains the original message, error details, and metadata for troubleshooting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessage {

    @JsonProperty("originalMessage")
    private String originalMessage;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("partition")
    private Integer partition;

    @JsonProperty("offset")
    private Long offset;

    @JsonProperty("errorType")
    private String errorType;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("stackTrace")
    private String stackTrace;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("retryCount")
    private Integer retryCount;
}
