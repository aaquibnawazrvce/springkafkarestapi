# cURL & PowerShell Command Reference

This document provides ready-to-use commands for testing and interacting with the Kafka REST Bridge microservice.

## Table of Contents
- [Health & Monitoring](#health--monitoring)
- [Kafka Operations](#kafka-operations)
- [Mock REST API Testing](#mock-rest-api-testing)
- [Docker Operations](#docker-operations)

---

## Health & Monitoring

### Check Application Health

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" | ConvertTo-Json -Depth 10
```

**curl:**
```bash
curl http://localhost:8080/actuator/health | jq
```

### Check Detailed Health (Including Kafka)

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" | ConvertTo-Json -Depth 10
```

**curl:**
```bash
curl http://localhost:8080/actuator/health | jq
```

### Get Application Metrics

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics"
```

**curl:**
```bash
curl http://localhost:8080/actuator/metrics | jq
```

### Get Specific Metric (Kafka Consumer)

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/kafka.consumer.fetch.manager.records.consumed.total"
```

**curl:**
```bash
curl http://localhost:8080/actuator/metrics/kafka.consumer.fetch.manager.records.consumed.total | jq
```

---

## Kafka Operations

### Send Valid Message to Kafka

**PowerShell:**
```powershell
Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
```

**Linux/Mac:**
```bash
cat sample-data/valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
```

### Send Invalid Message (Goes to DLQ)

**PowerShell:**
```powershell
Get-Content sample-data\invalid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
```

**Linux/Mac:**
```bash
cat sample-data/invalid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
```

### Send Multiple Messages

**PowerShell:**
```powershell
Get-ChildItem sample-data\valid-*.json | ForEach-Object {
    Get-Content $_.FullName | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
    Write-Host "Sent $($_.Name)"
}
```

**Linux/Mac:**
```bash
for file in sample-data/valid-*.json; do
  cat "$file" | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
  echo "Sent $file"
done
```

### Consume Messages from Input Topic

**PowerShell:**
```powershell
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic input-messages --from-beginning --timeout-ms 5000
```

### Consume Messages from DLQ

**PowerShell:**
```powershell
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning --timeout-ms 5000
```

### List All Topics

**PowerShell:**
```powershell
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Describe a Topic

**PowerShell:**
```powershell
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic input-messages
```

### Check Consumer Group Status

**PowerShell:**
```powershell
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe
```

### Check Consumer Lag

**PowerShell:**
```powershell
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe | Select-String "LAG"
```

### Delete All Messages from Topic (Reset)

**PowerShell:**
```powershell
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic input-messages
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --create --topic input-messages --replication-factor 1 --partitions 3
```

---

## Mock REST API Testing

### Test Mock API Directly

**PowerShell:**
```powershell
$body = @{
    transaction_id = "TEST-001"
    event_name = "TEST_EVENT"
    timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
    customer = @{
        id = "CUST-999"
        full_name = "Test User"
        contact_email = "test@example.com"
        contact_phone = "+1234567890"
        is_active = $true
    }
    transaction = @{
        amount = 100.50
        currency_code = "USD"
        notes = "Test transaction"
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/process" -Method Post -Body $body -ContentType "application/json" | ConvertTo-Json
```

**curl:**
```bash
curl -X POST http://localhost:8081/api/v1/process \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "TEST-001",
    "event_name": "TEST_EVENT",
    "timestamp": "2025-12-24T10:30:00",
    "customer": {
      "id": "CUST-999",
      "full_name": "Test User",
      "contact_email": "test@example.com",
      "contact_phone": "+1234567890",
      "is_active": true
    },
    "transaction": {
      "amount": 100.50,
      "currency_code": "USD",
      "notes": "Test transaction"
    }
  }' | jq
```

---

## Docker Operations

### Start All Services

**PowerShell:**
```powershell
docker-compose up -d
```

### Stop All Services

**PowerShell:**
```powershell
docker-compose down
```

### Stop and Remove Volumes (Clean Slate)

**PowerShell:**
```powershell
docker-compose down -v
```

### View Logs (All Services)

**PowerShell:**
```powershell
docker-compose logs -f
```

### View Logs (Specific Service)

**PowerShell:**
```powershell
docker-compose logs -f kafka-rest-bridge
```

### View Last 50 Lines of Logs

**PowerShell:**
```powershell
docker-compose logs --tail=50 kafka-rest-bridge
```

### Check Container Status

**PowerShell:**
```powershell
docker-compose ps
```

### Restart a Service

**PowerShell:**
```powershell
docker-compose restart kafka-rest-bridge
```

### Scale the Application

**PowerShell:**
```powershell
docker-compose up -d --scale kafka-rest-bridge=3
```

### Execute Command in Container

**PowerShell:**
```powershell
docker exec -it kafka-rest-bridge /bin/sh
```

### View Container Resource Usage

**PowerShell:**
```powershell
docker stats kafka-rest-bridge
```

---

## Complete Testing Workflow

### End-to-End Test Script

**PowerShell:**
```powershell
# 1. Start services
Write-Host "Starting services..." -ForegroundColor Cyan
docker-compose up -d

# 2. Wait for startup
Write-Host "Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# 3. Check health
Write-Host "Checking health..." -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
Write-Host "Health status: $($health.status)" -ForegroundColor Green

# 4. Send valid message
Write-Host "Sending valid message..." -ForegroundColor Cyan
Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 5. Wait for processing
Start-Sleep -Seconds 3

# 6. Check logs for success
Write-Host "Checking logs..." -ForegroundColor Cyan
docker-compose logs kafka-rest-bridge | Select-String "Message processed successfully" | Select-Object -Last 1

# 7. Send invalid message
Write-Host "Sending invalid message (should go to DLQ)..." -ForegroundColor Cyan
Get-Content sample-data\invalid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages

# 8. Wait for processing
Start-Sleep -Seconds 3

# 9. Check DLQ
Write-Host "Checking DLQ..." -ForegroundColor Cyan
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning --timeout-ms 3000

# 10. Check consumer lag
Write-Host "Checking consumer lag..." -ForegroundColor Cyan
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe

Write-Host "Test complete!" -ForegroundColor Green
```

---

## Bulk Operations

### Send 100 Messages for Load Testing

**PowerShell:**
```powershell
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

for ($i=1; $i -le 100; $i++) {
    Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages
    if ($i % 10 -eq 0) {
        Write-Host "Sent $i messages..."
    }
}

$stopwatch.Stop()
Write-Host "Sent 100 messages in $($stopwatch.Elapsed.TotalSeconds) seconds" -ForegroundColor Green
```

### Clear All DLQ Messages

**PowerShell:**
```powershell
# Reset DLQ offsets to latest (skip all current messages)
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group dlq-consumer --reset-offsets --to-latest --topic dlq-messages --execute
```

---

## Monitoring Commands

### Watch Logs in Real-Time with Filtering

**PowerShell:**
```powershell
docker-compose logs -f kafka-rest-bridge | Select-String "ERROR|WARN|Message processed"
```

### Count Messages Processed

**PowerShell:**
```powershell
(docker-compose logs kafka-rest-bridge | Select-String "Message processed successfully").Count
```

### Count Validation Failures

**PowerShell:**
```powershell
(docker-compose logs kafka-rest-bridge | Select-String "Validation failed").Count
```

### Monitor Consumer Lag Continuously

**PowerShell:**
```powershell
while ($true) {
    Clear-Host
    docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe
    Start-Sleep -Seconds 5
}
```

---

## Troubleshooting Commands

### Check if Kafka is Ready

**PowerShell:**
```powershell
docker exec -it kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Test Network Connectivity

**PowerShell:**
```powershell
docker exec kafka-rest-bridge ping -c 3 kafka
docker exec kafka-rest-bridge ping -c 3 mock-api
```

### Check Port Availability

**PowerShell:**
```powershell
Test-NetConnection -ComputerName localhost -Port 8080
Test-NetConnection -ComputerName localhost -Port 9092
Test-NetConnection -ComputerName localhost -Port 8081
```

### View Environment Variables

**PowerShell:**
```powershell
docker exec kafka-rest-bridge env | Select-String "KAFKA|REST_API"
```

---

## Quick Reference Card

| Action | Command |
|--------|---------|
| Start all | `docker-compose up -d` |
| Stop all | `docker-compose down` |
| View logs | `docker-compose logs -f kafka-rest-bridge` |
| Send message | `Get-Content sample-data\valid-message-1.json \| docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages` |
| Check health | `Invoke-RestMethod http://localhost:8080/actuator/health` |
| View DLQ | `docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic dlq-messages --from-beginning` |
| Check lag | `docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-rest-bridge-group --describe` |
| Kafka UI | Open http://localhost:8090 |

---

**Note**: Replace `localhost` with your Docker host IP if running on a remote machine.

**Last Updated**: December 24, 2025
