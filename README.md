# Kafka REST Bridge Microservice

A production-ready Spring Boot 3 microservice that consumes JSON messages from Apache Kafka, validates them against JSON schemas, transforms the data, and forwards it to an external REST API with comprehensive error handling and retry mechanisms.

## 🏗️ Architecture Overview

```
┌─────────────┐      ┌──────────────────────────────────────────┐      ┌─────────────┐
│   Kafka     │      │     Kafka REST Bridge Service            │      │  External   │
│   Topic     │─────▶│                                          │─────▶│  REST API   │
│ (JSON msgs) │      │  ┌────────────┐  ┌──────────────────┐   │      │             │
└─────────────┘      │  │  Consumer  │─▶│   Validator      │   │      └─────────────┘
                     │  │  Listener  │  │ (JSON Schema +   │   │              │
                     │  └────────────┘  │  Bean Validation)│   │              │
                     │         │        └──────────────────┘   │              │
                     │         ▼               │                │              │
                     │    ┌─────────┐          │ Valid          │              │
                     │    │   DLQ   │◀─────────┘                │              │
                     │    │  Topic  │   Invalid                 │              │
                     │    └─────────┘                           │              │
                     │         ▲               │                │              │
                     │         │               ▼                │              │
                     │         │      ┌──────────────────┐     │              │
                     │         │      │  Transformation  │     │              │
                     │         │      │     Service      │     │              │
                     │         │      └──────────────────┘     │              │
                     │         │               │                │              │
                     │         │               ▼                │              │
                     │         │      ┌──────────────────┐     │              │
                     │         │      │   REST Client    │     │              │
                     │         │      │ (Exponential     │     │              │
                     │         └──────│  Backoff Retry)  │     │              │
                     │      API Failure└──────────────────┘     │              │
                     │                                          │              │
                     └──────────────────────────────────────────┘              │
```

## 📋 Table of Contents

- [Features](#features)
- [Technologies](#technologies)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Running Tests](#running-tests)
- [Docker Deployment](#docker-deployment)
- [API Documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Architecture Details](#architecture-details)
- [Troubleshooting](#troubleshooting)

## ✨ Features

- ✅ **Kafka Consumer**: Consumes JSON messages from Kafka topics with configurable concurrency
- ✅ **Dual Validation**: JSON Schema validation + Bean Validation (JSR-380)
- ✅ **Data Transformation**: Maps Kafka message structure to REST API contract
- ✅ **Retry Logic**: Exponential backoff retry with Resilience4j
- ✅ **Dead Letter Queue**: Failed messages routed to DLQ with error details
- ✅ **Health Checks**: Spring Boot Actuator endpoints for monitoring
- ✅ **Externalized Configuration**: Environment-based configuration via application.yml
- ✅ **Authentication Support**: Bearer token and Basic auth for REST API
- ✅ **Container Ready**: Dockerfile and docker-compose for easy deployment
- ✅ **Comprehensive Testing**: Unit tests, integration tests with embedded Kafka

## 🛠️ Technologies

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Kafka 3.1.1**
- **Resilience4j** (Retry & Circuit Breaker)
- **JSON Schema Validator 1.0.87**
- **Maven**
- **Docker & Docker Compose**
- **Apache Kafka 7.5.0**
- **WireMock** (for testing)
- **Testcontainers** (for integration tests)

## 📦 Prerequisites

- **Java 17+** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **Docker & Docker Compose** ([Download](https://www.docker.com/products/docker-desktop))
- **Git** (optional, for version control)

## 🚀 Quick Start

### 1. Clone or Navigate to Project Directory

```bash
cd c:\LEARNING\AIGENERATEDAPPS\springkafkarestapi
```

### 2. Build the Project

```bash
mvn clean package -DskipTests
```

### 3. Start Infrastructure with Docker Compose

```bash
docker-compose up -d
```

This starts:
- Zookeeper (port 2181)
- Kafka (port 9092)
- Kafka UI (port 8090)
- Mock REST API (port 8081)
- The application (port 8080)

### 4. Verify Services

```bash
# Check all containers are running
docker-compose ps

# View application logs
docker-compose logs -f kafka-rest-bridge

# Access Kafka UI
# Open browser: http://localhost:8090

# Check application health
curl http://localhost:8080/actuator/health
```

### 5. Send Test Messages

```bash
# Send a valid message
docker exec -it kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# Paste this JSON (then Ctrl+D to send):
{"messageId":"MSG-001","eventType":"PAYMENT_CREATED","timestamp":"2025-12-24T10:30:00","payload":{"customerId":"CUST-123","customerName":"John Doe","email":"john.doe@example.com","phone":"+1234567890","amount":100.50,"currency":"USD","description":"Test payment","active":true}}
```

## ⚙️ Configuration

### Environment Variables

All configuration can be overridden via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker addresses |
| `KAFKA_CONSUMER_GROUP` | `kafka-rest-bridge-group` | Consumer group ID |
| `KAFKA_INPUT_TOPIC` | `input-messages` | Topic to consume from |
| `KAFKA_DLQ_TOPIC` | `dlq-messages` | Dead letter queue topic |
| `KAFKA_CONCURRENCY` | `3` | Number of consumer threads |
| `REST_API_BASE_URL` | `http://localhost:8081` | Target REST API base URL |
| `REST_API_ENDPOINT` | `/api/v1/process` | API endpoint path |
| `REST_API_RETRY_MAX_ATTEMPTS` | `3` | Max retry attempts |
| `REST_API_RETRY_INITIAL_INTERVAL` | `1000` | Initial retry delay (ms) |
| `REST_API_RETRY_MULTIPLIER` | `2.0` | Backoff multiplier |
| `REST_API_AUTH_ENABLED` | `false` | Enable authentication |
| `REST_API_AUTH_TYPE` | `bearer` | Auth type: `bearer` or `basic` |
| `REST_API_AUTH_TOKEN` | - | Bearer token (if enabled) |
| `LOG_LEVEL` | `INFO` | Application log level |

### application.yml

The main configuration file is located at `src/main/resources/application.yml`. 

Key sections:
- **Spring Kafka**: Consumer/producer settings
- **App Config**: Topics, REST API settings, validation
- **Actuator**: Health check endpoints
- **Logging**: Log levels and patterns

## 🧪 Running Tests

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### Test Coverage

```bash
mvn clean verify
# Coverage report: target/site/jacoco/index.html
```

### Test Structure

- `TransformationServiceTest`: Tests message transformation logic
- `ValidationServiceTest`: Tests JSON Schema and Bean validation
- `RestApiClientTest`: Tests REST client with WireMock
- `KafkaIntegrationTest`: End-to-end tests with embedded Kafka

## 🐳 Docker Deployment

### Build Docker Image

```bash
docker build -t kafka-rest-bridge:latest .
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# Scale the application
docker-compose up -d --scale kafka-rest-bridge=3

# View logs
docker-compose logs -f kafka-rest-bridge

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Environment-Specific Deployment

For production, create a `docker-compose.prod.yml`:

```yaml
version: '3.8'
services:
  kafka-rest-bridge:
    image: kafka-rest-bridge:latest
    environment:
      KAFKA_BOOTSTRAP_SERVERS: prod-kafka:9092
      REST_API_BASE_URL: https://api.production.com
      REST_API_AUTH_ENABLED: true
      REST_API_AUTH_TOKEN: ${API_TOKEN}
      LOG_LEVEL: WARN
```

Run with:
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 📊 API Documentation

### Message Schema

#### Incoming Kafka Message (IncomingMessage)

```json
{
  "messageId": "MSG-001",
  "eventType": "PAYMENT_CREATED",
  "timestamp": "2025-12-24T10:30:00",
  "payload": {
    "customerId": "CUST-123",
    "customerName": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "amount": 100.50,
    "currency": "USD",
    "description": "Optional description",
    "active": true
  },
  "metadata": {
    "key": "value"
  }
}
```

**Validation Rules:**
- `messageId`: Required, non-blank
- `eventType`: Required, non-blank
- `timestamp`: Required, ISO 8601 format
- `payload.customerId`: Required
- `payload.customerName`: Required, 2-100 characters
- `payload.email`: Required, valid email format
- `payload.phone`: Required, E.164 format
- `payload.amount`: Required, > 0.01
- `payload.currency`: Required, 3-letter ISO code
- `payload.active`: Required, boolean

#### Transformed REST API Request (ApiRequestDto)

```json
{
  "transaction_id": "MSG-001",
  "event_name": "PAYMENT_CREATED",
  "timestamp": "2025-12-24T10:30:00",
  "customer": {
    "id": "CUST-123",
    "full_name": "John Doe",
    "contact_email": "john.doe@example.com",
    "contact_phone": "+1234567890",
    "is_active": true
  },
  "transaction": {
    "amount": 100.50,
    "currency_code": "USD",
    "notes": "Optional description"
  }
}
```

#### REST API Response (ApiResponseDto)

```json
{
  "success": true,
  "message": "Processed successfully",
  "transaction_id": "MSG-001",
  "reference_id": "REF-12345",
  "status_code": 200
}
```

#### Dead Letter Queue Message

```json
{
  "originalMessage": "{...original JSON...}",
  "topic": "input-messages",
  "partition": 0,
  "offset": 12345,
  "errorType": "ValidationException",
  "errorMessage": "Email must be valid",
  "stackTrace": "...",
  "timestamp": "2025-12-24T10:35:00",
  "retryCount": 0
}
```

## 📈 Monitoring

### Health Checks

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Detailed health with Kafka status
curl http://localhost:8080/actuator/health | jq
```

### Metrics

```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Kafka consumer metrics
curl http://localhost:8080/actuator/metrics/kafka.consumer.fetch.manager.records.consumed.total
```

### Application Info

```bash
curl http://localhost:8080/actuator/info
```

### Kafka UI

Access Kafka UI at: `http://localhost:8090`

Features:
- View topics and messages
- Monitor consumer groups
- Check lag and offset
- Browse DLQ messages

## 🏛️ Architecture Details

### Component Flow

1. **Kafka Consumer** (`MessageConsumer`)
   - Listens to configured input topic
   - Deserializes JSON to `IncomingMessage` POJO
   - Manual offset commit for reliability

2. **Validation Service** (`ValidationService`)
   - JSON Schema validation (structural)
   - Bean Validation (business rules)
   - Fail-fast option for efficiency

3. **Transformation Service** (`TransformationService`)
   - Maps Kafka message format to API format
   - Handles null values gracefully
   - Maintains traceability (messageId → transactionId)

4. **REST API Client** (`RestApiClient`)
   - Configurable authentication (Bearer/Basic)
   - Exponential backoff retry
   - Differentiates transient (5xx) vs permanent (4xx) errors

5. **Dead Letter Queue Service** (`DeadLetterQueueService`)
   - Captures failed messages
   - Enriches with error metadata
   - Prevents message loss

### Error Handling Strategy

| Error Type | Behavior | Example |
|------------|----------|---------|
| **Deserialization Error** | → DLQ | Malformed JSON |
| **Validation Error** | → DLQ | Invalid email format |
| **4xx API Error** | → DLQ (no retry) | Bad request, unauthorized |
| **5xx API Error** | Retry → DLQ | Service unavailable |
| **Network Timeout** | Retry → DLQ | Connection timeout |

### Retry Configuration

```yaml
max-attempts: 3
initial-interval: 1000ms
multiplier: 2.0
```

**Retry Timeline:**
- Attempt 1: Immediate
- Attempt 2: +1 second
- Attempt 3: +2 seconds
- Total max: ~3 seconds before DLQ

## 🔧 Troubleshooting

### Issue: Consumer Not Receiving Messages

**Check:**
```bash
# Verify Kafka is running
docker-compose ps kafka

# Check topics exist
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer group
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe
```

### Issue: Messages Going to DLQ

**Debug:**
1. Check DLQ topic:
```bash
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning
```

2. Review application logs:
```bash
docker-compose logs kafka-rest-bridge | grep ERROR
```

3. Common causes:
   - Invalid JSON format
   - Missing required fields
   - Type mismatches
   - REST API unreachable

### Issue: REST API Not Responding

**Check:**
```bash
# Test mock API
curl http://localhost:8081/api/v1/process -X POST -H "Content-Type: application/json" -d '{"test":"data"}'

# Check network connectivity
docker exec kafka-rest-bridge ping mock-api
```

### Issue: High Memory Usage

**Solution:**
Adjust JVM settings in Dockerfile:
```dockerfile
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=50.0", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-jar", "app.jar"]
```

## 📝 Sample Messages

### Valid Message

```json
{
  "messageId": "MSG-20251224-001",
  "eventType": "PAYMENT_CREATED",
  "timestamp": "2025-12-24T14:30:00",
  "payload": {
    "customerId": "CUST-5678",
    "customerName": "Jane Smith",
    "email": "jane.smith@example.com",
    "phone": "+447911123456",
    "amount": 250.75,
    "currency": "GBP",
    "description": "Online purchase",
    "active": true
  },
  "metadata": {
    "source": "web-app",
    "version": "v2.1"
  }
}
```

### Invalid Message (for DLQ testing)

```json
{
  "messageId": "MSG-INVALID",
  "eventType": "TEST",
  "timestamp": "2025-12-24T14:30:00",
  "payload": {
    "customerId": "CUST-9999",
    "customerName": "X",
    "email": "not-an-email",
    "phone": "invalid",
    "amount": -10,
    "currency": "US",
    "active": "maybe"
  }
}
```

## 🧰 Development

### Local Development (without Docker)

1. Start Kafka locally or use Confluent Cloud
2. Update `application.yml` with Kafka connection details
3. Run the application:

```bash
mvn spring-boot:run
```

### IDE Setup

**IntelliJ IDEA:**
1. Import as Maven project
2. Enable annotation processing (Lombok)
3. Set JDK to 17
4. Run `KafkaRestBridgeApplication`

**VS Code:**
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Open project folder
4. Run via Spring Boot Dashboard

## 📚 Additional Resources

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Resilience4j Guide](https://resilience4j.readme.io/)
- [JSON Schema Specification](https://json-schema.org/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add/update tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 👥 Support

For issues and questions:
- Check [Troubleshooting](#troubleshooting) section
- Review application logs
- Create an issue in the repository

---

**Built with ❤️ using Spring Boot and Apache Kafka**
