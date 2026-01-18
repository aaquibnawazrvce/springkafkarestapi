# Testing Guide

This guide provides detailed instructions for testing the Kafka REST Bridge microservice.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Starting the Environment](#starting-the-environment)
- [Sending Test Messages](#sending-test-messages)
- [Verifying Results](#verifying-results)
- [Testing Scenarios](#testing-scenarios)
- [Using Postman](#using-postman)

## Prerequisites

Ensure all services are running:

```powershell
docker-compose ps
```

All services should show "Up" status.

## Starting the Environment

```powershell
# Start all services
docker-compose up -d

# Wait for services to be ready (about 30 seconds)
Start-Sleep -Seconds 30

# Check health
curl http://localhost:8080/actuator/health
```

## Sending Test Messages

### Method 1: Using kafka-console-producer

```powershell
# Send a valid message
Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# Send multiple valid messages
Get-Content sample-data\valid-message-2.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
Get-Content sample-data\valid-message-3.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# Send invalid messages (will go to DLQ)
Get-Content sample-data\invalid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
Get-Content sample-data\invalid-message-2.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
```

### Method 2: Using Kafka UI

1. Open browser: http://localhost:8090
2. Navigate to Topics → input-messages
3. Click "Produce Message"
4. Paste JSON from sample-data files
5. Click "Produce Message"

### Method 3: Interactive Console Producer

```powershell
# Start interactive producer
docker exec -it kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# Paste JSON message and press Enter
# Press Ctrl+C to exit
```

## Verifying Results

### 1. Check Application Logs

```powershell
# View real-time logs
docker-compose logs -f kafka-rest-bridge

# Search for specific message
docker-compose logs kafka-rest-bridge | Select-String "MSG-20251224-001"

# Check for errors
docker-compose logs kafka-rest-bridge | Select-String "ERROR"
```

### 2. Verify REST API Calls

Check WireMock logs to see API calls:

```powershell
docker-compose logs mock-api | Select-String "POST /api/v1/process"
```

### 3. Check DLQ Messages

```powershell
# View DLQ messages
docker exec -it kafka kafka-console-consumer `
    --bootstrap-server localhost:9092 `
    --topic dlq-messages `
    --from-beginning `
    --timeout-ms 5000

# Count DLQ messages
docker exec -it kafka kafka-run-class kafka.tools.GetOffsetShell `
    --broker-list localhost:9092 `
    --topic dlq-messages
```

### 4. Monitor Consumer Group

```powershell
# Check consumer group status
docker exec -it kafka kafka-consumer-groups `
    --bootstrap-server localhost:9092 `
    --group kafka-rest-bridge-group `
    --describe

# Check lag
docker exec -it kafka kafka-consumer-groups `
    --bootstrap-server localhost:9092 `
    --group kafka-rest-bridge-group `
    --describe | Select-String "LAG"
```

## Testing Scenarios

### Scenario 1: Valid Message Processing

**Objective**: Verify successful processing of valid messages

```powershell
# 1. Send valid message
Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 2. Wait 2 seconds
Start-Sleep -Seconds 2

# 3. Check logs for success
docker-compose logs kafka-rest-bridge | Select-String "Message processed successfully"

# 4. Verify REST API was called
docker-compose logs mock-api | Select-String "POST"

# 5. Verify no DLQ messages
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning --timeout-ms 2000
```

**Expected Result**: 
- Log shows "Message processed successfully"
- REST API received POST request
- No message in DLQ

### Scenario 2: Invalid Message Handling

**Objective**: Verify invalid messages are sent to DLQ

```powershell
# 1. Send invalid message
Get-Content sample-data\invalid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 2. Wait 2 seconds
Start-Sleep -Seconds 2

# 3. Check for validation error in logs
docker-compose logs kafka-rest-bridge | Select-String "Validation failed"

# 4. Verify message in DLQ
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning --timeout-ms 2000
```

**Expected Result**:
- Log shows "Validation failed"
- Message appears in DLQ with error details
- REST API was NOT called

### Scenario 3: REST API Failure & Retry

**Objective**: Verify retry logic when REST API fails

```powershell
# 1. Stop mock API to simulate failure
docker-compose stop mock-api

# 2. Send valid message
Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 3. Wait 10 seconds (for retries)
Start-Sleep -Seconds 10

# 4. Check logs for retry attempts
docker-compose logs kafka-rest-bridge | Select-String "retry"

# 5. Verify message in DLQ after retries exhausted
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning --timeout-ms 2000

# 6. Restart mock API
docker-compose start mock-api
```

**Expected Result**:
- Multiple retry attempts in logs
- Message eventually sent to DLQ
- Error type indicates connection failure

### Scenario 4: High Volume Processing

**Objective**: Verify service can handle multiple messages

```powershell
# Send 10 valid messages
for ($i=1; $i -le 10; $i++) {
    Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
    Write-Host "Sent message $i"
}

# Wait 5 seconds
Start-Sleep -Seconds 5

# Check consumer lag (should be 0)
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe

# Count processed messages in logs
docker-compose logs kafka-rest-bridge | Select-String "Message processed successfully" | Measure-Object
```

**Expected Result**:
- All 10 messages processed
- Consumer lag returns to 0
- No errors in logs

### Scenario 5: Service Recovery

**Objective**: Verify service recovers after restart

```powershell
# 1. Send messages while service is running
Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 2. Restart the service
docker-compose restart kafka-rest-bridge

# 3. Wait for service to start
Start-Sleep -Seconds 20

# 4. Send another message
Get-Content sample-data\valid-message-2.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 5. Verify both messages were processed
docker-compose logs kafka-rest-bridge | Select-String "Message processed successfully"
```

**Expected Result**:
- Service restarts successfully
- Messages processed after restart
- No message loss

## Using Postman

### Import Collection

1. Open Postman
2. Click Import
3. Select file: `sample-data/Kafka-REST-Bridge.postman_collection.json`
4. Click Import

### Available Requests

**Health & Monitoring:**
- Health Check: GET http://localhost:8080/actuator/health
- Detailed Health: Includes Kafka connectivity status
- Application Info: Application metadata
- Metrics: Performance metrics

**Mock REST API:**
- Process Message: Test the target API endpoint directly

### Environment Variables

Create a Postman environment with:
- `baseUrl`: http://localhost:8080
- `mockApiUrl`: http://localhost:8081

## Automated Testing

### Run Unit Tests

```powershell
mvn test
```

### Run Integration Tests

```powershell
mvn verify
```

### Run Specific Test

```powershell
mvn test -Dtest=ValidationServiceTest
mvn test -Dtest=KafkaIntegrationTest
```

## Performance Testing

### Measure Processing Time

```powershell
# Send 100 messages and measure time
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
for ($i=1; $i -le 100; $i++) {
    Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
}
$stopwatch.Stop()
Write-Host "Time taken: $($stopwatch.Elapsed.TotalSeconds) seconds"

# Wait for processing
Start-Sleep -Seconds 10

# Check if all processed
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe
```

## Troubleshooting Tests

### Logs Not Showing Processing

```powershell
# Check if consumer is connected
docker-compose logs kafka-rest-bridge | Select-String "Started MessageConsumer"

# Check Kafka connectivity
docker-compose logs kafka-rest-bridge | Select-String "Kafka"
```

### Messages Not Reaching Kafka

```powershell
# List all topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Verify topic has messages
docker exec -it kafka kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic input-messages
```

### DLQ Messages Not Visible

```powershell
# Check if DLQ topic exists
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic dlq-messages

# Consume from beginning
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning --max-messages 10
```

## Clean Up After Testing

```powershell
# Stop all services
docker-compose down

# Remove all data (topics, messages)
docker-compose down -v

# Restart fresh
docker-compose up -d
```

## Test Coverage Report

Generate and view test coverage:

```powershell
# Run tests with coverage
mvn clean verify

# View report (if using JaCoCo)
Start-Process "target\site\jacoco\index.html"
```

---

**Happy Testing! 🧪**
