# Architecture Documentation

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         External Systems                             │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
         ┌──────────────────┐           ┌──────────────────┐
         │  Kafka Cluster   │           │   REST API       │
         │  (Input Topic)   │           │   (External)     │
         └──────────────────┘           └──────────────────┘
                    │                               ▲
                    │                               │
                    ▼                               │
┌────────────────────────────────────────────────────────────────────┐
│                  Kafka REST Bridge Microservice                    │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │                    Consumer Layer                         │    │
│  │  ┌────────────────┐    ┌────────────────┐               │    │
│  │  │ Kafka Listener │───▶│ Deserialization│               │    │
│  │  │  (@Consumer)   │    │   (JSON→POJO)  │               │    │
│  │  └────────────────┘    └────────────────┘               │    │
│  └──────────────────────────────────────────────────────────┘    │
│                              │                                     │
│  ┌──────────────────────────▼───────────────────────────────┐    │
│  │                   Validation Layer                        │    │
│  │  ┌─────────────────┐    ┌──────────────────┐            │    │
│  │  │ JSON Schema     │    │ Bean Validation  │            │    │
│  │  │  Validator      │───▶│   (JSR-380)      │            │    │
│  │  └─────────────────┘    └──────────────────┘            │    │
│  └──────────────────────────────────────────────────────────┘    │
│                              │                                     │
│                    ┌─────────┴─────────┐                          │
│                    │    Valid?         │                          │
│                    └─────────┬─────────┘                          │
│                    No        │         Yes                        │
│                    │         │                                    │
│                    ▼         ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │               Transformation Layer                        │    │
│  │  ┌─────────────────────────────────────────┐             │    │
│  │  │  Message Transformer                    │             │    │
│  │  │  (IncomingMessage → ApiRequestDto)      │             │    │
│  │  └─────────────────────────────────────────┘             │    │
│  └──────────────────────────────────────────────────────────┘    │
│                              │                                     │
│  ┌──────────────────────────▼───────────────────────────────┐    │
│  │                   REST Client Layer                       │    │
│  │  ┌─────────────────┐    ┌──────────────────┐            │    │
│  │  │  RestTemplate   │───▶│ Retry Logic      │───────┐    │    │
│  │  │   + Auth        │    │ (Resilience4j)   │       │    │    │
│  │  └─────────────────┘    └──────────────────┘       │    │    │
│  └──────────────────────────────────────────────────────────┘    │
│                              │                           │        │
│                      Success │                    Failure│        │
│                              │                           │        │
│  ┌──────────────────────────▼───────────────────────────▼───┐    │
│  │                  Error Handling Layer                    │    │
│  │  ┌─────────────────────────────────────────┐            │    │
│  │  │  Dead Letter Queue Service               │            │    │
│  │  │  - Enriches with error metadata          │            │    │
│  │  │  - Prevents message loss                 │            │    │
│  │  └─────────────────────────────────────────┘            │    │
│  └──────────────────────────────────────────────────────────┘    │
│                              │                                     │
└──────────────────────────────┼─────────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │  Kafka Cluster   │
                    │   (DLQ Topic)    │
                    └──────────────────┘
```

## Component Details

### 1. Consumer Layer

**Components:**
- `MessageConsumer`: Main Kafka listener
- `KafkaConsumerConfig`: Consumer configuration

**Responsibilities:**
- Listen to input Kafka topic
- Deserialize JSON messages to POJOs
- Manage offset commits manually for reliability
- Route messages to validation layer

**Configuration:**
- Concurrency: 3 threads (configurable)
- Manual acknowledgment mode
- Error handling deserializer wrapper

### 2. Validation Layer

**Components:**
- `ValidationService`: Orchestrates validation
- JSON Schema Validator (networknt)
- Jakarta Bean Validator

**Responsibilities:**
- Validate message structure (JSON Schema)
- Validate business rules (Bean Validation)
- Reject malformed or invalid messages
- Log validation failures with details

**Validation Flow:**
```
Message → JSON Schema Check → Bean Validation → Valid/Invalid
              ↓                      ↓
          Structure            Business Rules
```

### 3. Transformation Layer

**Components:**
- `TransformationService`: Message mapper

**Responsibilities:**
- Map `IncomingMessage` to `ApiRequestDto`
- Handle field name transformations
- Manage null values gracefully
- Maintain traceability (messageId → transactionId)

**Mapping Examples:**
```
customerName     → full_name
email            → contact_email
amount           → transaction.amount
currency         → transaction.currency_code
```

### 4. REST Client Layer

**Components:**
- `RestApiClient`: HTTP client wrapper
- `RestTemplate`: Spring HTTP client
- Resilience4j Retry: Retry mechanism

**Responsibilities:**
- Send HTTP POST requests
- Handle authentication (Bearer/Basic)
- Implement exponential backoff retry
- Differentiate transient vs permanent errors

**Retry Strategy:**
```
Attempt 1: Immediate
Attempt 2: +1 second
Attempt 3: +2 seconds
Total: ~3 seconds max before giving up
```

**Error Classification:**
- `4xx`: Permanent error → No retry → DLQ
- `5xx`: Transient error → Retry → DLQ if exhausted
- Network errors: Retry → DLQ if exhausted

### 5. Error Handling Layer

**Components:**
- `DeadLetterQueueService`: DLQ manager

**Responsibilities:**
- Capture all failed messages
- Enrich with error metadata
- Publish to DLQ topic
- Prevent message loss

**DLQ Message Structure:**
```json
{
  "originalMessage": "...",
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

## Data Flow

### Happy Path

```
1. Message arrives in Kafka topic
   ↓
2. Consumer deserializes JSON to IncomingMessage
   ↓
3. ValidationService validates structure & business rules
   ↓
4. TransformationService maps to ApiRequestDto
   ↓
5. RestApiClient sends POST with retry logic
   ↓
6. Success response received
   ↓
7. Offset committed
   ↓
8. Message processing complete
```

### Error Path (Validation Failure)

```
1. Message arrives in Kafka topic
   ↓
2. Consumer deserializes JSON to IncomingMessage
   ↓
3. ValidationService validates → FAILS
   ↓
4. DeadLetterQueueService publishes to DLQ
   ↓
5. Offset committed (to prevent reprocessing)
   ↓
6. Original message preserved in DLQ with error details
```

### Error Path (REST API Failure)

```
1. Message arrives in Kafka topic
   ↓
2. Consumer deserializes JSON
   ↓
3. ValidationService validates → PASS
   ↓
4. TransformationService maps to ApiRequestDto
   ↓
5. RestApiClient sends POST → FAILS (5xx)
   ↓
6. Retry attempt 1 → FAILS
   ↓
7. Retry attempt 2 → FAILS
   ↓
8. DeadLetterQueueService publishes to DLQ
   ↓
9. Offset committed
   ↓
10. Message preserved in DLQ with error details
```

## Configuration Architecture

### Externalized Configuration

All configuration is externalized through:
- `application.yml` (defaults)
- Environment variables (overrides)
- Command-line arguments (highest priority)

### Configuration Hierarchy

```
application.yml
      ↓
Environment Variables
      ↓
Command-line Args
```

### Key Configuration Groups

1. **Kafka Configuration**
   - Bootstrap servers
   - Consumer group
   - Topics (input, DLQ)
   - Concurrency

2. **REST API Configuration**
   - Base URL
   - Endpoint
   - Timeouts
   - Authentication

3. **Retry Configuration**
   - Max attempts
   - Initial interval
   - Multiplier
   - Max interval

4. **Validation Configuration**
   - JSON schema path
   - Fail-fast mode

## Deployment Architecture

### Docker Compose Stack

```
┌─────────────────────────────────────────────┐
│           Docker Network (Bridge)           │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │Zookeeper │  │  Kafka   │  │ Kafka UI │ │
│  │  :2181   │◀─│  :9092   │──│  :8090   │ │
│  └──────────┘  └──────────┘  └──────────┘ │
│                     ▲                       │
│                     │                       │
│  ┌─────────────────┴──────┐  ┌──────────┐ │
│  │  Kafka REST Bridge     │  │Mock API  │ │
│  │       :8080            │─▶│  :8081   │ │
│  │  - Health: :8080/health│  │          │ │
│  └────────────────────────┘  └──────────┘ │
│                                             │
└─────────────────────────────────────────────┘
```

### Health Checks

All containers have health checks:
- **Zookeeper**: Port 2181 connectivity
- **Kafka**: Broker API availability
- **Mock API**: Admin endpoint
- **Application**: Spring Actuator health

### Scaling

Horizontal scaling is supported:

```bash
docker-compose up -d --scale kafka-rest-bridge=3
```

Each instance:
- Joins the same consumer group
- Processes different partitions
- Shares the load automatically

## Security Architecture

### Authentication Support

1. **Bearer Token Authentication**
   ```yaml
   REST_API_AUTH_ENABLED: true
   REST_API_AUTH_TYPE: bearer
   REST_API_AUTH_TOKEN: your-token-here
   ```

2. **Basic Authentication**
   ```yaml
   REST_API_AUTH_ENABLED: true
   REST_API_AUTH_TYPE: basic
   REST_API_USERNAME: user
   REST_API_PASSWORD: pass
   ```

### Container Security

- Non-root user (uid: 1001)
- Minimal base image (Alpine)
- No unnecessary capabilities
- Read-only root filesystem (can be configured)

## Monitoring Architecture

### Observability Stack

```
┌────────────────────────────────────────┐
│      Application Metrics               │
│  ┌──────────────────────────────────┐  │
│  │  Spring Boot Actuator            │  │
│  │  - /actuator/health              │  │
│  │  - /actuator/metrics             │  │
│  │  - /actuator/prometheus          │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
              │
              ▼
    (External monitoring tools)
    - Prometheus (metrics collection)
    - Grafana (visualization)
    - ELK Stack (log aggregation)
```

### Key Metrics

- Kafka consumer lag
- Message processing rate
- REST API success/failure rate
- DLQ message count
- Retry attempts

### Logging

Structured logging with:
- Correlation IDs (messageId)
- Log levels (DEBUG, INFO, WARN, ERROR)
- Contextual information
- Timestamp with milliseconds

## Performance Considerations

### Throughput

Default configuration handles:
- **~100 messages/second** (single instance)
- **~300 messages/second** (3 instances)

Factors affecting throughput:
- Kafka partition count
- Consumer concurrency
- REST API response time
- Network latency

### Optimization Tips

1. **Increase Concurrency**
   ```yaml
   KAFKA_CONCURRENCY: 5
   ```

2. **Tune Max Poll Records**
   ```yaml
   KAFKA_MAX_POLL_RECORDS: 20
   ```

3. **Optimize REST API Timeout**
   ```yaml
   REST_API_READ_TIMEOUT: 5000
   ```

4. **Scale Horizontally**
   ```bash
   docker-compose up -d --scale kafka-rest-bridge=5
   ```

## Failure Modes

### Kafka Unavailable

- Application health check fails
- Consumer connection retries automatically
- Messages queued in Kafka
- No message loss

### REST API Unavailable

- Retry logic activates
- After max retries, message → DLQ
- Consumer continues with next message
- No blocking or message loss

### Database Unavailable (if added)

- Consider using Kafka as source of truth
- Implement eventual consistency
- Use DLQ for replay

### Service Crash

- Kafka retains uncommitted offsets
- Service restarts and resumes from last commit
- No message loss (if offset management correct)

## Disaster Recovery

### Backup Strategy

1. **Kafka Topics**: Configured with replication
2. **DLQ Messages**: Retained indefinitely for analysis
3. **Configuration**: Stored in version control
4. **Logs**: Archived to external storage

### Recovery Procedures

1. **Message Loss Investigation**
   - Check DLQ topic
   - Review application logs
   - Analyze consumer group lag

2. **Service Recovery**
   - Restart from last committed offset
   - Replay DLQ messages if needed
   - Monitor consumer lag

## Future Enhancements

Potential architectural improvements:

1. **Event Sourcing**: Store all events for audit
2. **Circuit Breaker**: Advanced failure handling
3. **Rate Limiting**: Prevent API overload
4. **Caching**: Reduce duplicate REST calls
5. **Batch Processing**: Send multiple messages per API call
6. **Schema Registry**: Centralized schema management
7. **Distributed Tracing**: OpenTelemetry integration

---

**Last Updated**: December 24, 2025
