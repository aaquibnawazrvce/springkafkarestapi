# Project Structure

```
springkafkarestapi/
│
├── src/
│   ├── main/
│   │   ├── java/com/example/kafka/
│   │   │   ├── KafkaRestBridgeApplication.java    # Main application entry point
│   │   │   │
│   │   │   ├── config/                             # Configuration classes
│   │   │   │   ├── ApplicationProperties.java     # Externalized configuration properties
│   │   │   │   ├── KafkaConsumerConfig.java       # Kafka consumer setup
│   │   │   │   ├── KafkaProducerConfig.java       # Kafka producer (DLQ) setup
│   │   │   │   └── RestClientConfig.java          # REST client & retry configuration
│   │   │   │
│   │   │   ├── consumer/                           # Kafka consumer layer
│   │   │   │   └── MessageConsumer.java           # Main Kafka listener
│   │   │   │
│   │   │   ├── service/                            # Business logic layer
│   │   │   │   ├── ValidationService.java         # JSON Schema + Bean validation
│   │   │   │   ├── TransformationService.java     # Message transformation
│   │   │   │   ├── RestApiClient.java             # REST API client with retry
│   │   │   │   ├── MessageProcessingService.java  # Orchestration service
│   │   │   │   └── DeadLetterQueueService.java    # DLQ message handling
│   │   │   │
│   │   │   ├── model/                              # Data models
│   │   │   │   ├── IncomingMessage.java           # Kafka message POJO
│   │   │   │   ├── ApiRequestDto.java             # REST API request DTO
│   │   │   │   ├── ApiResponseDto.java            # REST API response DTO
│   │   │   │   └── DlqMessage.java                # DLQ message wrapper
│   │   │   │
│   │   │   └── exception/                          # Custom exceptions
│   │   │       ├── ValidationException.java       # Validation failures
│   │   │       └── RestApiException.java          # REST API failures
│   │   │
│   │   └── resources/
│   │       ├── application.yml                     # Main configuration file
│   │       └── schema/
│   │           └── message-schema.json            # JSON Schema for validation
│   │
│   └── test/
│       ├── java/com/example/kafka/
│       │   ├── service/                            # Service unit tests
│       │   │   ├── TransformationServiceTest.java
│       │   │   ├── ValidationServiceTest.java
│       │   │   └── RestApiClientTest.java
│       │   │
│       │   └── integration/                        # Integration tests
│       │       └── KafkaIntegrationTest.java      # End-to-end tests
│       │
│       └── resources/
│           └── application-test.yml               # Test configuration
│
├── sample-data/                                    # Sample test data
│   ├── valid-message-1.json
│   ├── valid-message-2.json
│   ├── valid-message-3.json
│   ├── invalid-message-1.json
│   ├── invalid-message-2.json
│   ├── invalid-message-3.json
│   └── Kafka-REST-Bridge.postman_collection.json  # Postman collection
│
├── wiremock/                                       # WireMock configuration
│   └── mappings/
│       └── api-success.json                       # Mock API response
│
├── pom.xml                                         # Maven dependencies
├── Dockerfile                                      # Container image definition
├── docker-compose.yml                              # Multi-container setup
├── .gitignore                                      # Git ignore rules
│
├── README.md                                       # Main documentation
├── ARCHITECTURE.md                                 # Architecture details
├── TESTING.md                                      # Testing guide
└── quick-start.ps1                                # Quick start script
```

## Component Descriptions

### Source Code (`src/main/java`)

#### Main Application
- **KafkaRestBridgeApplication.java**: Spring Boot application entry point with `@EnableRetry`

#### Configuration (`config/`)
- **ApplicationProperties.java**: Type-safe configuration properties mapping to `application.yml`
- **KafkaConsumerConfig.java**: Kafka consumer factory and listener container configuration
- **KafkaProducerConfig.java**: Kafka producer factory for DLQ messages
- **RestClientConfig.java**: RestTemplate bean and Resilience4j retry configuration

#### Consumer Layer (`consumer/`)
- **MessageConsumer.java**: Kafka listener that orchestrates the entire message processing flow

#### Service Layer (`service/`)
- **ValidationService.java**: Validates messages using JSON Schema and Bean Validation
- **TransformationService.java**: Transforms Kafka messages to REST API format
- **RestApiClient.java**: HTTP client with authentication and retry logic
- **MessageProcessingService.java**: Coordinates transformation and REST API calls
- **DeadLetterQueueService.java**: Publishes failed messages to DLQ with error metadata

#### Model Layer (`model/`)
- **IncomingMessage.java**: POJO for Kafka messages with validation annotations
- **ApiRequestDto.java**: DTO for REST API requests
- **ApiResponseDto.java**: DTO for REST API responses
- **DlqMessage.java**: Wrapper for DLQ messages with error details

#### Exception Layer (`exception/`)
- **ValidationException.java**: Thrown when validation fails
- **RestApiException.java**: Thrown when REST API calls fail

### Resources (`src/main/resources`)
- **application.yml**: Main configuration with externalized properties
- **schema/message-schema.json**: JSON Schema for structural validation

### Tests (`src/test`)

#### Unit Tests (`service/`)
- **TransformationServiceTest.java**: Tests message transformation logic
- **ValidationServiceTest.java**: Tests JSON Schema and Bean validation
- **RestApiClientTest.java**: Tests REST client with WireMock

#### Integration Tests (`integration/`)
- **KafkaIntegrationTest.java**: End-to-end tests with embedded Kafka and WireMock

### Sample Data (`sample-data/`)
- **valid-message-*.json**: Valid message examples for testing
- **invalid-message-*.json**: Invalid message examples for DLQ testing
- **Kafka-REST-Bridge.postman_collection.json**: Postman collection for API testing

### Docker Configuration
- **Dockerfile**: Multi-stage build for production-ready container
- **docker-compose.yml**: Complete stack with Kafka, Zookeeper, Mock API, and application
- **wiremock/mappings/**: WireMock stub configurations

### Documentation
- **README.md**: Comprehensive project documentation
- **ARCHITECTURE.md**: Detailed architecture documentation
- **TESTING.md**: Testing guide with PowerShell commands
- **quick-start.ps1**: Automated setup script

### Build Configuration
- **pom.xml**: Maven project definition with all dependencies
- **.gitignore**: Files and directories to exclude from version control

## Key Dependency Highlights

### Runtime Dependencies
- Spring Boot 3.2.1
- Spring Kafka 3.1.1
- Spring Web (RestTemplate)
- Spring Validation (Bean Validation)
- Resilience4j (Retry logic)
- JSON Schema Validator
- Jackson (JSON processing)
- Lombok (Boilerplate reduction)

### Test Dependencies
- Spring Boot Test
- Spring Kafka Test (Embedded Kafka)
- Testcontainers (Container-based testing)
- WireMock (HTTP mocking)
- Awaitility (Async testing)
- JUnit 5
- AssertJ (Fluent assertions)

## Configuration Files

### application.yml Structure
```yaml
spring:
  application: ...
  kafka:
    bootstrap-servers: ...
    consumer: ...
    producer: ...
    listener: ...
  
app:
  kafka:
    topic: ...
  rest-api:
    base-url: ...
    endpoint: ...
    timeout: ...
    retry: ...
    auth: ...
  validation:
    json-schema-path: ...

management:
  endpoints: ...
  
logging:
  level: ...
```

### docker-compose.yml Services
1. **zookeeper**: Kafka coordination
2. **kafka**: Message broker
3. **kafka-ui**: Web UI for Kafka
4. **init-kafka**: Topic creation
5. **mock-api**: WireMock for testing
6. **kafka-rest-bridge**: The main application

## Entry Points

### Running Locally (IDE)
```
KafkaRestBridgeApplication.main()
```

### Running with Maven
```bash
mvn spring-boot:run
```

### Running with Docker
```bash
docker-compose up
```

### Quick Start
```powershell
.\quick-start.ps1
```

## Build Artifacts

After `mvn clean package`:
```
target/
├── spring-kafka-rest-api-1.0.0.jar          # Executable JAR
├── classes/                                   # Compiled classes
├── test-classes/                             # Test classes
└── maven-archiver/                           # Build metadata
```

## Port Mappings

| Service | Port | Purpose |
|---------|------|---------|
| Application | 8080 | Main service |
| Kafka | 9092 | Kafka broker |
| Zookeeper | 2181 | Kafka coordination |
| Kafka UI | 8090 | Kafka management UI |
| Mock API | 8081 | Test REST API |

## Environment Variables Reference

See README.md for complete list of configurable environment variables.

---

**Last Updated**: December 24, 2025
