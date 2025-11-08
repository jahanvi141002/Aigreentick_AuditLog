# Setup and Test Script - Checks and starts all required services
# Run this script to ensure everything is ready before testing

Write-Host "=== Service Setup and Test Script ===" -ForegroundColor Cyan
Write-Host ""

# Function to check if a port is in use
function Test-Port {
    param([int]$Port)
    $connection = Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
    return $connection
}

# Step 1: Check MongoDB
Write-Host "[1/6] Checking MongoDB..." -ForegroundColor Yellow
try {
    $mongoRunning = Test-Port -Port 27017
    if ($mongoRunning) {
        Write-Host "✅ MongoDB is running on port 27017" -ForegroundColor Green
    } else {
        Write-Host "❌ MongoDB is not running on port 27017" -ForegroundColor Red
        Write-Host "   Attempting to start MongoDB service..." -ForegroundColor Yellow
        try {
            Start-Service MongoDB -ErrorAction Stop
            Start-Sleep -Seconds 3
            Write-Host "✅ MongoDB service started" -ForegroundColor Green
        } catch {
            Write-Host "❌ Failed to start MongoDB. Please start it manually:" -ForegroundColor Red
            Write-Host "   Start-Service MongoDB" -ForegroundColor Yellow
            Write-Host "   Or check if MongoDB is installed and configured." -ForegroundColor Yellow
            exit 1
        }
    }
} catch {
    Write-Host "⚠️  Could not check MongoDB status" -ForegroundColor Yellow
}

# Step 2: Check Kafka
Write-Host "`n[2/6] Checking Kafka..." -ForegroundColor Yellow
try {
    $kafkaRunning = Test-Port -Port 9092
    if ($kafkaRunning) {
        Write-Host "✅ Kafka is running on port 9092" -ForegroundColor Green
    } else {
        Write-Host "❌ Kafka is not running on port 9092" -ForegroundColor Red
        Write-Host "   Attempting to start Kafka..." -ForegroundColor Yellow
        
        # Check if start script exists
        if (Test-Path ".\start-kafka-kraft.ps1") {
            Write-Host "   Running start-kafka-kraft.ps1 in new window..." -ForegroundColor Gray
            Write-Host "   (Kafka may take 20-30 seconds to start, especially on first run)" -ForegroundColor Gray
            Start-Process powershell -ArgumentList "-NoExit", "-File", ".\start-kafka-kraft.ps1" -WindowStyle Normal
            
            # Wait longer for Kafka to start (it can take time, especially on first run)
            Write-Host "   Waiting 25 seconds for Kafka to start..." -ForegroundColor Gray
            Start-Sleep -Seconds 25
            
            # Check multiple times with retries
            $kafkaStarted = $false
            for ($i = 1; $i -le 3; $i++) {
                $kafkaRunning = Test-Port -Port 9092
                if ($kafkaRunning) {
                    $kafkaStarted = $true
                    break
                }
                Write-Host "   Still waiting... (attempt $i/3)" -ForegroundColor Gray
                Start-Sleep -Seconds 5
            }
            
            if ($kafkaStarted) {
                Write-Host "✅ Kafka started successfully" -ForegroundColor Green
            } else {
                Write-Host "⚠️  Kafka may still be starting. Check the Kafka window for status." -ForegroundColor Yellow
                Write-Host "   You can continue, but tests may fail if Kafka isn't ready." -ForegroundColor Yellow
                Write-Host "   To start manually: .\start-kafka-kraft.ps1" -ForegroundColor Cyan
            }
        } else {
            Write-Host "❌ start-kafka-kraft.ps1 not found. Please start Kafka manually." -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "⚠️  Could not check Kafka status" -ForegroundColor Yellow
}

# Step 3: Check Spring Boot Application
Write-Host "`n[3/6] Checking Spring Boot Application..." -ForegroundColor Yellow
try {
    $appRunning = Test-Port -Port 8081
    if ($appRunning) {
        Write-Host "✅ Spring Boot application is running on port 8081" -ForegroundColor Green
        
        # Test if it's responding
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 5 -ErrorAction Stop
            Write-Host "✅ Application is responding" -ForegroundColor Green
        } catch {
            Write-Host "⚠️  Application is running but not responding. It may still be starting up." -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Spring Boot application is not running on port 8081" -ForegroundColor Red
        Write-Host "   Starting Spring Boot application..." -ForegroundColor Yellow
        
        # Check if Maven is available
        try {
            $mvnVersion = mvn --version 2>&1 | Select-Object -First 1
            if ($mvnVersion -match "Apache Maven") {
                Write-Host "   Maven found. Starting application in new window..." -ForegroundColor Gray
                Write-Host "   (Application may take 30-60 seconds to start, especially on first run)" -ForegroundColor Gray
                Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; Write-Host 'Starting Spring Boot Application...' -ForegroundColor Green; mvn spring-boot:run" -WindowStyle Normal
                Write-Host "   Waiting 45 seconds for application to start..." -ForegroundColor Gray
                Start-Sleep -Seconds 45
                
                # Check multiple times with retries
                $appStarted = $false
                for ($i = 1; $i -le 5; $i++) {
                    $appRunning = Test-Port -Port 8081
                    if ($appRunning) {
                        # Also test if API is responding
                        try {
                            $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 3 -ErrorAction Stop
                            $appStarted = $true
                            break
                        } catch {
                            Write-Host "   Port is open but API not ready yet... (attempt $i/5)" -ForegroundColor Gray
                            Start-Sleep -Seconds 5
                        }
                    } else {
                        Write-Host "   Still waiting... (attempt $i/5)" -ForegroundColor Gray
                        Start-Sleep -Seconds 5
                    }
                }
                
                if ($appStarted) {
                    Write-Host "✅ Application started successfully" -ForegroundColor Green
                } else {
                    Write-Host "⚠️  Application may still be starting. Check the application window." -ForegroundColor Yellow
                    Write-Host "   Look for 'Started AuditApplication' message in the logs." -ForegroundColor Yellow
                    Write-Host "   To start manually: mvn spring-boot:run" -ForegroundColor Cyan
                }
            } else {
                Write-Host "❌ Maven not found. Please start the application manually:" -ForegroundColor Red
                Write-Host "   mvn spring-boot:run" -ForegroundColor Yellow
                exit 1
            }
        } catch {
            Write-Host "❌ Maven not found. Please start the application manually:" -ForegroundColor Red
            Write-Host "   mvn spring-boot:run" -ForegroundColor Yellow
            exit 1
        }
    }
} catch {
    Write-Host "⚠️  Could not check application status" -ForegroundColor Yellow
}

# Step 4: Verify MongoDB Connection
Write-Host "`n[4/6] Verifying MongoDB connection..." -ForegroundColor Yellow
try {
    $mongoTest = mongosh --quiet --eval "db.version()" 2>&1
    if ($mongoTest -match "version") {
        Write-Host "✅ MongoDB connection verified" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Could not verify MongoDB connection" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  MongoDB shell not found or connection failed" -ForegroundColor Yellow
    Write-Host "   This is okay if MongoDB is running but mongosh is not in PATH" -ForegroundColor Gray
}

# Step 5: Verify Kafka Topics
Write-Host "`n[5/6] Verifying Kafka topics..." -ForegroundColor Yellow
try {
    # Check if Kafka bin directory exists
    $kafkaPath = "C:\Kafka\kafka_2.13-4.1.0\bin\windows"
    if (Test-Path $kafkaPath) {
        $topics = & "$kafkaPath\kafka-topics.bat" --list --bootstrap-server localhost:9092 2>&1
        if ($topics -match "audit-logs" -or $topics -match "exception-logs") {
            Write-Host "✅ Kafka topics found" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Topics not found yet (they will be auto-created)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠️  Kafka bin directory not found at expected path" -ForegroundColor Yellow
        Write-Host "   Topics will be auto-created when first used" -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️  Could not verify Kafka topics" -ForegroundColor Yellow
    Write-Host "   Topics will be auto-created when first used" -ForegroundColor Gray
}

# Step 6: Test API Endpoint
Write-Host "`n[6/6] Testing API endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ API endpoint is responding" -ForegroundColor Green
    Write-Host "   Current audit logs: $($response.Count)" -ForegroundColor Gray
} catch {
    Write-Host "❌ API endpoint is not responding" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
    Write-Host "   The application may still be starting. Wait a bit longer and try again." -ForegroundColor Yellow
    exit 1
}

# Summary
Write-Host "`n=== Setup Complete ===" -ForegroundColor Cyan
Write-Host "✅ All services are ready!" -ForegroundColor Green
Write-Host "`nYou can now run the test script:" -ForegroundColor White
Write-Host "   .\test-everything.ps1" -ForegroundColor Yellow
Write-Host ""

# Ask if user wants to run tests now
$runTests = Read-Host "Do you want to run the test suite now? (Y/N)"
if ($runTests -eq "Y" -or $runTests -eq "y") {
    Write-Host "`nRunning test suite..." -ForegroundColor Cyan
    & ".\test-everything.ps1"
} else {
    Write-Host "`nSetup complete. Run .\test-everything.ps1 when ready." -ForegroundColor Green
}

