# Quick Start Script - Starts everything and runs tests
# This is the simplest way to get everything running

Write-Host "=== Quick Start - Audit & Exception Logging ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Start MongoDB
Write-Host "[1/3] Starting MongoDB..." -ForegroundColor Yellow
try {
    $mongoRunning = Test-NetConnection -ComputerName localhost -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($mongoRunning) {
        Write-Host "✅ MongoDB is already running" -ForegroundColor Green
    } else {
        Start-Service MongoDB -ErrorAction Stop
        Start-Sleep -Seconds 3
        Write-Host "✅ MongoDB started" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Failed to start MongoDB. Please start manually: Start-Service MongoDB" -ForegroundColor Red
    exit 1
}

# Step 2: Start Kafka
Write-Host "`n[2/3] Starting Kafka..." -ForegroundColor Yellow
try {
    $kafkaRunning = Test-NetConnection -ComputerName localhost -Port 9092 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($kafkaRunning) {
        Write-Host "✅ Kafka is already running" -ForegroundColor Green
    } else {
        if (Test-Path ".\start-kafka-kraft.ps1") {
            Write-Host "   Launching Kafka (this may take 20-30 seconds)..." -ForegroundColor Gray
            Start-Process powershell -ArgumentList "-NoExit", "-File", ".\start-kafka-kraft.ps1" -WindowStyle Normal
            Write-Host "   Waiting 30 seconds for Kafka..." -ForegroundColor Gray
            Start-Sleep -Seconds 30
            
            # Verify
            $kafkaRunning = Test-NetConnection -ComputerName localhost -Port 9092 -InformationLevel Quiet -WarningAction SilentlyContinue
            if ($kafkaRunning) {
                Write-Host "✅ Kafka started" -ForegroundColor Green
            } else {
                Write-Host "⚠️  Kafka may still be starting. Check the Kafka window." -ForegroundColor Yellow
            }
        } else {
            Write-Host "❌ start-kafka-kraft.ps1 not found" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "❌ Failed to start Kafka" -ForegroundColor Red
    exit 1
}

# Step 3: Start Spring Boot Application
Write-Host "`n[3/3] Starting Spring Boot Application..." -ForegroundColor Yellow
try {
    $appRunning = Test-NetConnection -ComputerName localhost -Port 8081 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($appRunning) {
        # Test if API responds
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 3 -ErrorAction Stop
            Write-Host "✅ Application is already running and responding" -ForegroundColor Green
        } catch {
            Write-Host "⚠️  Port is open but API not responding. Restarting..." -ForegroundColor Yellow
            $appRunning = $false
        }
    }
    
    if (-not $appRunning) {
        Write-Host "   Launching application (this may take 30-60 seconds)..." -ForegroundColor Gray
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; Write-Host 'Starting Spring Boot...' -ForegroundColor Green; mvn spring-boot:run" -WindowStyle Normal
        Write-Host "   Waiting 60 seconds for application to start..." -ForegroundColor Gray
        Write-Host "   (Watch the application window for 'Started AuditApplication' message)" -ForegroundColor Gray
        Start-Sleep -Seconds 60
        
        # Verify with retries
        $appReady = $false
        for ($i = 1; $i -le 6; $i++) {
            try {
                $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 3 -ErrorAction Stop
                $appReady = $true
                break
            } catch {
                Write-Host "   Still waiting for API... ($i/6)" -ForegroundColor Gray
                Start-Sleep -Seconds 10
            }
        }
        
        if ($appReady) {
            Write-Host "✅ Application started and responding" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Application may still be starting. Check the application window." -ForegroundColor Yellow
            Write-Host "   Look for 'Started AuditApplication' in the logs." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ Failed to start application" -ForegroundColor Red
    Write-Host "   Start manually: mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Final verification
Write-Host "`n=== Verifying All Services ===" -ForegroundColor Cyan
$allReady = $true

$mongoOk = Test-NetConnection -ComputerName localhost -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue
$kafkaOk = Test-NetConnection -ComputerName localhost -Port 9092 -InformationLevel Quiet -WarningAction SilentlyContinue

try {
    $apiOk = $false
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 3 -ErrorAction Stop
    $apiOk = $true
} catch {
    $apiOk = $false
}

if ($mongoOk) { Write-Host "✅ MongoDB: Running" -ForegroundColor Green } else { Write-Host "❌ MongoDB: Not running" -ForegroundColor Red; $allReady = $false }
if ($kafkaOk) { Write-Host "✅ Kafka: Running" -ForegroundColor Green } else { Write-Host "⚠️  Kafka: May still be starting" -ForegroundColor Yellow }
if ($apiOk) { Write-Host "✅ Spring Boot: Running and responding" -ForegroundColor Green } else { Write-Host "⚠️  Spring Boot: May still be starting" -ForegroundColor Yellow; $allReady = $false }

if ($allReady) {
    Write-Host "`n✅ All services are ready!" -ForegroundColor Green
    Write-Host ""
    
    # Ask to run tests
    $runTests = Read-Host "Run test suite now? (Y/N)"
    if ($runTests -eq "Y" -or $runTests -eq "y") {
        Write-Host "`nRunning tests..." -ForegroundColor Cyan
        & ".\test-everything.ps1"
    } else {
        Write-Host "`nServices are running. Run tests when ready:" -ForegroundColor White
        Write-Host "   .\test-everything.ps1" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n⚠️  Some services may still be starting." -ForegroundColor Yellow
    Write-Host "Wait a bit longer, then check with: .\check-services.ps1" -ForegroundColor Cyan
    Write-Host "Or run tests: .\test-everything.ps1" -ForegroundColor Cyan
}

