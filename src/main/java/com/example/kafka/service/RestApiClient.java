package com.example.kafka.service;

import com.example.kafka.config.ApplicationProperties;
import com.example.kafka.exception.RestApiException;
import com.example.kafka.model.ApiRequestDto;
import com.example.kafka.model.ApiResponseDto;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

/**
 * Service responsible for making REST API calls with retry logic and authentication.
 */
@Slf4j
@Service
public class RestApiClient {

    private final RestTemplate restTemplate;
    private final ApplicationProperties properties;
    private final Retry retry;

    public RestApiClient(RestTemplate restTemplate,
                        ApplicationProperties properties,
                        Retry restApiRetry) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.retry = restApiRetry;
    }

    /**
     * Sends the API request to the configured REST endpoint with retry logic.
     *
     * @param apiRequest The request DTO to send
     * @return The response from the API
     * @throws RestApiException if the API call fails after all retries
     */
    public ApiResponseDto sendRequest(ApiRequestDto apiRequest) throws RestApiException {
        String url = buildUrl();
        
        log.info("Sending request to REST API: {} for transaction: {}", 
                url, apiRequest.getTransactionId());

        Supplier<ApiResponseDto> supplier = Retry.decorateSupplier(retry, () -> {
            try {
                HttpHeaders headers = createHeaders();
                HttpEntity<ApiRequestDto> entity = new HttpEntity<>(apiRequest, headers);

                ResponseEntity<ApiResponseDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        ApiResponseDto.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("Successfully sent request to REST API. Transaction: {}, Status: {}",
                            apiRequest.getTransactionId(), response.getStatusCode());
                    return response.getBody();
                } else {
                    throw new RuntimeException("Unexpected response from API: " + response.getStatusCode());
                }

            } catch (HttpClientErrorException e) {
                log.error("Client error calling REST API (4xx): {} - {}", 
                        e.getStatusCode(), e.getResponseBodyAsString());
                
                // Don't retry for client errors (4xx)
                if (e.getStatusCode().value() >= 400 && e.getStatusCode().value() < 500) {
                    throw new RuntimeException("Non-retryable client error: " + e.getStatusCode(), e);
                }
                throw e;

            } catch (HttpServerErrorException e) {
                log.warn("Server error calling REST API (5xx): {} - {}. Will retry...", 
                        e.getStatusCode(), e.getResponseBodyAsString());
                throw e; // Retry for server errors

            } catch (Exception e) {
                log.error("Error calling REST API: {}", e.getMessage(), e);
                throw e;
            }
        });

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Failed to send request to REST API after retries. Transaction: {}",
                    apiRequest.getTransactionId(), e);
            
            Integer statusCode = extractStatusCode(e);
            throw new RestApiException(
                    "Failed to send request to REST API: " + e.getMessage(),
                    statusCode,
                    e
            );
        }
    }

    private String buildUrl() {
        String baseUrl = properties.getRestApi().getBaseUrl();
        String endpoint = properties.getRestApi().getEndpoint();
        
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl + endpoint.substring(1);
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }
        return baseUrl + endpoint;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (properties.getRestApi().getAuth().isEnabled()) {
            String authType = properties.getRestApi().getAuth().getType();
            
            if ("bearer".equalsIgnoreCase(authType)) {
                String token = properties.getRestApi().getAuth().getToken();
                headers.setBearerAuth(token);
                log.debug("Added Bearer token authentication");
                
            } else if ("basic".equalsIgnoreCase(authType)) {
                String username = properties.getRestApi().getAuth().getUsername();
                String password = properties.getRestApi().getAuth().getPassword();
                headers.setBasicAuth(username, password);
                log.debug("Added Basic authentication");
            }
        }

        return headers;
    }

    private Integer extractStatusCode(Exception e) {
        if (e instanceof HttpClientErrorException) {
            return ((HttpClientErrorException) e).getStatusCode().value();
        } else if (e instanceof HttpServerErrorException) {
            return ((HttpServerErrorException) e).getStatusCode().value();
        }
        return null;
    }
}
