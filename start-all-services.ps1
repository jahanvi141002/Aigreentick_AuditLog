# Start All Services Script
# This script starts MongoDB, Kafka, and the Spring Boot application

Write-Host "=== Starting All Services ===" -ForegroundColor Cyan
Write-Host ""

# Function to check if a port is in use
function Test-Port {
    param([int]$Port)
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
        return $connection
    } catch {
        return $false
    }
}

# 1. Start MongoDB
Write-Host "[1/3] Starting MongoDB..." -ForegroundColor Yellow
if (Test-Port -Port 27017) {
    Write-Host "✅ MongoDB is already running" -ForegroundColor Green
} else {
    try {
        Start-Service MongoDB -ErrorAction Stop
        Start-Sleep -Seconds 3
        if (Test-Port -Port 27017) {
            Write-Host "✅ MongoDB started successfully" -ForegroundColor Green
        } else {
            Write-Host "❌ MongoDB failed to start" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Failed to start MongoDB: $_" -ForegroundColor Red
        Write-Host "   Try: Start-Service MongoDB" -ForegroundColor Yellow
    }
}

# 2. Start Kafka
Write-Host "`n[2/3] Starting Kafka..." -ForegroundColor Yellow
if (Test-Port -Port 9092) {
    Write-Host "✅ Kafka is already running" -ForegroundColor Green
} else {
    if (Test-Path ".\start-kafka-kraft.ps1") {
        Write-Host "   Launching Kafka in new window..." -ForegroundColor Gray
        Start-Process powershell -ArgumentList "-NoExit", "-File", ".\start-kafka-kraft.ps1" -WindowStyle Minimized
        Write-Host "   Waiting 10 seconds for Kafka to start..." -ForegroundColor Gray
        Start-Sleep -Seconds 10
        
        if (Test-Port -Port 9092) {
            Write-Host "✅ Kafka started successfully" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Kafka may still be starting. Check the Kafka window." -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ start-kafka-kraft.ps1 not found" -ForegroundColor Red
        Write-Host "   Please start Kafka manually" -ForegroundColor Yellow
    }
}

# 3. Start Spring Boot Application
Write-Host "`n[3/3] Starting Spring Boot Application..." -ForegroundColor Yellow
if (Test-Port -Port 8081) {
    Write-Host "✅ Application is already running" -ForegroundColor Green
} else {
    try {
        $mvnCheck = mvn --version 2>&1 | Select-Object -First 1
        if ($mvnCheck -match "Apache Maven") {
            Write-Host "   Launching application in new window..." -ForegroundColor Gray
            Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run" -WindowStyle Normal
            Write-Host "   Waiting 30 seconds for application to start..." -ForegroundColor Gray
            Write-Host "   (This may take longer on first run)" -ForegroundColor Gray
            Start-Sleep -Seconds 30
            
            if (Test-Port -Port 8081) {
                Write-Host "✅ Application started successfully" -ForegroundColor Green
            } else {
                Write-Host "⚠️  Application may still be starting. Check the application window." -ForegroundColor Yellow
                Write-Host "   Wait for 'Started AuditApplication' message" -ForegroundColor Gray
            }
        } else {
            Write-Host "❌ Maven not found" -ForegroundColor Red
            Write-Host "   Please start the application manually: mvn spring-boot:run" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ Failed to start application: $_" -ForegroundColor Red
        Write-Host "   Please start manually: mvn spring-boot:run" -ForegroundColor Yellow
    }
}

# Final Check
Write-Host "`n=== Final Status ===" -ForegroundColor Cyan
$mongoOk = Test-Port -Port 27017
$kafkaOk = Test-Port -Port 9092
$appOk = Test-Port -Port 8081

if ($mongoOk -and $kafkaOk -and $appOk) {
    Write-Host "✅ All services are running!" -ForegroundColor Green
    Write-Host ""
    Write-Host "You can now run tests:" -ForegroundColor White
    Write-Host "   .\test-everything.ps1" -ForegroundColor Yellow
} else {
    Write-Host "⚠️  Some services may still be starting:" -ForegroundColor Yellow
    if (-not $mongoOk) { Write-Host "   - MongoDB" -ForegroundColor Red }
    if (-not $kafkaOk) { Write-Host "   - Kafka" -ForegroundColor Red }
    if (-not $appOk) { Write-Host "   - Spring Boot Application" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Wait a bit longer and check with: .\check-services.ps1" -ForegroundColor Yellow
}

