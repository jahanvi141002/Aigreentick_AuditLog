# Quick Service Status Check Script
# Run this to quickly check if all services are running

Write-Host "=== Service Status Check ===" -ForegroundColor Cyan
Write-Host ""

function Test-Port {
    param([int]$Port, [string]$ServiceName)
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
        if ($connection) {
            Write-Host "✅ $ServiceName is running on port $Port" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $ServiceName is NOT running on port $Port" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName is NOT running on port $Port" -ForegroundColor Red
        return $false
    }
}

# Check MongoDB
$mongoOk = Test-Port -Port 27017 -ServiceName "MongoDB"

# Check Kafka
$kafkaOk = Test-Port -Port 9092 -ServiceName "Kafka"

# Check Spring Boot App
$appOk = Test-Port -Port 8081 -ServiceName "Spring Boot Application"

# Test API
Write-Host ""
Write-Host "Testing API endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -TimeoutSec 3 -ErrorAction Stop
    Write-Host "✅ API is responding" -ForegroundColor Green
} catch {
    Write-Host "❌ API is not responding" -ForegroundColor Red
    $appOk = $false
}

# Summary
Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
if ($mongoOk -and $kafkaOk -and $appOk) {
    Write-Host "✅ All services are running!" -ForegroundColor Green
    Write-Host "   You can run: .\test-everything.ps1" -ForegroundColor Yellow
} else {
    Write-Host "❌ Some services are not running" -ForegroundColor Red
    Write-Host ""
    Write-Host "To start services, run:" -ForegroundColor Yellow
    if (-not $mongoOk) {
        Write-Host "   Start-Service MongoDB" -ForegroundColor White
    }
    if (-not $kafkaOk) {
        Write-Host "   .\start-kafka-kraft.ps1" -ForegroundColor White
    }
    if (-not $appOk) {
        Write-Host "   mvn spring-boot:run" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "Or run the setup script:" -ForegroundColor Yellow
    Write-Host "   .\setup-and-test.ps1" -ForegroundColor White
}

