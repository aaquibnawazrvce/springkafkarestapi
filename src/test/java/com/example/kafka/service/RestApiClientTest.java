package com.example.kafka.service;

import com.example.kafka.config.ApplicationProperties;
import com.example.kafka.exception.RestApiException;
import com.example.kafka.model.ApiRequestDto;
import com.example.kafka.model.ApiResponseDto;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RestApiClientTest {

    private WireMockServer wireMockServer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Retry restApiRetry;

    private RestApiClient restApiClient;
    private ApplicationProperties properties;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);

        properties = new ApplicationProperties();
        properties.setRestApi(new ApplicationProperties.RestApi());
        properties.getRestApi().setBaseUrl("http://localhost:8081");
        properties.getRestApi().setEndpoint("/api/v1/process");
        
        ApplicationProperties.RestApi.Timeout timeout = new ApplicationProperties.RestApi.Timeout();
        timeout.setConnect(5000);
        timeout.setRead(10000);
        properties.getRestApi().setTimeout(timeout);

        ApplicationProperties.RestApi.Auth auth = new ApplicationProperties.RestApi.Auth();
        auth.setEnabled(false);
        properties.getRestApi().setAuth(auth);

        restApiClient = new RestApiClient(restTemplate, properties, restApiRetry);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldSuccessfullySendRequest() throws RestApiException {
        // Given
        ApiRequestDto request = createTestRequest();

        stubFor(post(urlEqualTo("/api/v1/process"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "message": "Processed successfully",
                                    "transaction_id": "TXN-001",
                                    "reference_id": "REF-12345",
                                    "status_code": 200
                                }
                                """)));

        // When
        ApiResponseDto response = restApiClient.sendRequest(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Processed successfully");
        assertThat(response.getTransactionId()).isEqualTo("TXN-001");

        verify(postRequestedFor(urlEqualTo("/api/v1/process"))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void shouldRetryOnServerError() {
        // Given
        ApiRequestDto request = createTestRequest();

        // First two calls fail with 500, third succeeds
        stubFor(post(urlEqualTo("/api/v1/process"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("First Retry"));

        stubFor(post(urlEqualTo("/api/v1/process"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("Second Retry"));

        stubFor(post(urlEqualTo("/api/v1/process"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "message": "Processed after retry",
                                    "transaction_id": "TXN-002",
                                    "status_code": 200
                                }
                                """)));

        // When
        assertThatThrownBy(() -> restApiClient.sendRequest(request))
                .isInstanceOf(RestApiException.class);

        // Verify retries happened (at least 2 attempts)
        verify(moreThanOrExactly(2), postRequestedFor(urlEqualTo("/api/v1/process")));
    }

    @Test
    void shouldNotRetryOnClientError() {
        // Given
        ApiRequestDto request = createTestRequest();

        stubFor(post(urlEqualTo("/api/v1/process"))
                .willReturn(aResponse().withStatus(400)));

        // When & Then
        assertThatThrownBy(() -> restApiClient.sendRequest(request))
                .isInstanceOf(RestApiException.class);

        // Verify only one attempt (no retry for 4xx)
        verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/process")));
    }

    @Test
    void shouldFailAfterMaxRetries() {
        // Given
        ApiRequestDto request = createTestRequest();

        stubFor(post(urlEqualTo("/api/v1/process"))
                .willReturn(aResponse().withStatus(503)));

        // When & Then
        assertThatThrownBy(() -> restApiClient.sendRequest(request))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("Failed to send request to REST API");
    }

    private ApiRequestDto createTestRequest() {
        ApiRequestDto.CustomerInfo customer = ApiRequestDto.CustomerInfo.builder()
                .id("CUST-123")
                .fullName("John Doe")
                .contactEmail("john@example.com")
                .contactPhone("+1234567890")
                .isActive(true)
                .build();

        ApiRequestDto.TransactionInfo transaction = ApiRequestDto.TransactionInfo.builder()
                .amount(100.50)
                .currencyCode("USD")
                .notes("Test transaction")
                .build();

        return ApiRequestDto.builder()
                .transactionId("TXN-001")
                .eventName("PAYMENT_CREATED")
                .customer(customer)
                .transaction(transaction)
                .timestamp("2025-12-24T10:30:00")
                .build();
    }
}
