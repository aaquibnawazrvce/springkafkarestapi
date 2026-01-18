package com.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class KafkaRestBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaRestBridgeApplication.class, args);
    }
}
