package com.example.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Kafka kafka = new Kafka();
    private RestApi restApi = new RestApi();
    private Validation validation = new Validation();

    @Data
    public static class Kafka {
        private Topic topic = new Topic();

        @Data
        public static class Topic {
            private String input;
            private String dlq;
        }
    }

    @Data
    public static class RestApi {
        private String baseUrl;
        private String endpoint;
        private Timeout timeout = new Timeout();
        private Retry retry = new Retry();
        private Auth auth = new Auth();

        @Data
        public static class Timeout {
            private int connect;
            private int read;
        }

        @Data
        public static class Retry {
            private int maxAttempts;
            private long initialInterval;
            private double multiplier;
            private long maxInterval;
        }

        @Data
        public static class Auth {
            private boolean enabled;
            private String type;
            private String token;
            private String username;
            private String password;
        }
    }

    @Data
    public static class Validation {
        private String jsonSchemaPath;
        private boolean failFast;
    }
}
