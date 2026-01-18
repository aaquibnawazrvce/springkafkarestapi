# Quick Start Script for Kafka REST Bridge
# Run this script to build and start the entire system

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Kafka REST Bridge - Quick Start" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found. Please install Java 17 or higher." -ForegroundColor Red
    exit 1
}

# Check Maven
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
    Write-Host "✓ Maven found: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Maven not found. Please install Maven 3.8+." -ForegroundColor Red
    exit 1
}

# Check Docker
try {
    $dockerVersion = docker --version
    Write-Host "✓ Docker found: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker not found. Please install Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check Docker Compose
try {
    $composeVersion = docker-compose --version
    Write-Host "✓ Docker Compose found: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Compose not found. Please install Docker Compose." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "All prerequisites satisfied!" -ForegroundColor Green
Write-Host ""

# Build the application
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building the application..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. Please check the errors above." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ Build successful!" -ForegroundColor Green
Write-Host ""

# Start Docker Compose
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Docker containers..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Docker containers." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ Containers started!" -ForegroundColor Green
Write-Host ""

# Wait for services to be ready
Write-Host "Waiting for services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Check health
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Checking service health..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$healthUrl = "http://localhost:8080/actuator/health"
try {
    $health = Invoke-RestMethod -Uri $healthUrl -Method Get
    if ($health.status -eq "UP") {
        Write-Host "✓ Application is healthy!" -ForegroundColor Green
    } else {
        Write-Host "⚠ Application status: $($health.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Health check failed. Service might still be starting..." -ForegroundColor Yellow
    Write-Host "  Check logs with: docker-compose logs -f kafka-rest-bridge" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  System is ready!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Available endpoints:" -ForegroundColor Cyan
Write-Host "  • Application Health: http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "  • Application Metrics: http://localhost:8080/actuator/metrics" -ForegroundColor White
Write-Host "  • Kafka UI: http://localhost:8090" -ForegroundColor White
Write-Host "  • Mock REST API: http://localhost:8081/api/v1/process" -ForegroundColor White
Write-Host ""

Write-Host "Quick commands:" -ForegroundColor Cyan
Write-Host "  • View logs: docker-compose logs -f kafka-rest-bridge" -ForegroundColor White
Write-Host "  • Stop services: docker-compose down" -ForegroundColor White
Write-Host "  • Send test message: Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages" -ForegroundColor White
Write-Host ""

Write-Host "To send a test message, run:" -ForegroundColor Yellow
Write-Host '  Get-Content sample-data\valid-message-1.json | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic input-messages' -ForegroundColor Gray
Write-Host ""

Write-Host "For detailed testing instructions, see TESTING.md" -ForegroundColor Cyan
Write-Host "For architecture details, see ARCHITECTURE.md" -ForegroundColor Cyan
Write-Host ""
Write-Host "Happy testing! 🚀" -ForegroundColor Green
