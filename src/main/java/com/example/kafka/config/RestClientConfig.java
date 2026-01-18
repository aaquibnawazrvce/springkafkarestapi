package com.example.kafka.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private final ApplicationProperties applicationProperties;

    public RestClientConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::clientHttpRequestFactory)
                .setConnectTimeout(Duration.ofMillis(
                        applicationProperties.getRestApi().getTimeout().getConnect()))
                .setReadTimeout(Duration.ofMillis(
                        applicationProperties.getRestApi().getTimeout().getRead()))
                .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(applicationProperties.getRestApi().getTimeout().getConnect());
        factory.setReadTimeout(applicationProperties.getRestApi().getTimeout().getRead());
        return factory;
    }

    @Bean
    public Retry restApiRetry() {
        ApplicationProperties.RestApi.Retry retryConfig = applicationProperties.getRestApi().getRetry();
        
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryConfig.getMaxAttempts())
                .waitDuration(Duration.ofMillis(retryConfig.getInitialInterval()))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(
                                retryConfig.getInitialInterval(),
                                retryConfig.getMultiplier()))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        return registry.retry("restApiRetry");
    }
}
