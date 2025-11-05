# PowerShell script to test the entire audit logging system from start to finish
# This script automates the complete testing workflow

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Complete Audit Logging System Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check MongoDB
Write-Host "Step 1: Checking MongoDB..." -ForegroundColor Yellow
$mongoRunning = $false

# Check if MongoDB service is running
try {
    $mongoService = Get-Service -Name "*mongo*" -ErrorAction SilentlyContinue
    if ($mongoService -and $mongoService.Status -eq "Running") {
        Write-Host "SUCCESS: MongoDB service is running" -ForegroundColor Green
        $mongoRunning = $true
    }
} catch {
    # Service check failed, try port check
}

# Check if port 27017 is listening
if (-not $mongoRunning) {
    try {
        $portCheck = netstat -ano | Select-String ":27017" | Select-String "LISTENING"
        if ($portCheck) {
            Write-Host "SUCCESS: MongoDB is listening on port 27017" -ForegroundColor Green
            $mongoRunning = $true
        }
    } catch {
        # Port check failed
    }
}

# Try to connect via mongosh if available
if (-not $mongoRunning) {
    try {
        $mongoResult = mongosh --eval "db.version()" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "SUCCESS: MongoDB is accessible via mongosh" -ForegroundColor Green
            $mongoRunning = $true
        }
    } catch {
        # mongosh not available or not working
    }
}

if (-not $mongoRunning) {
    Write-Host "ERROR: MongoDB is not accessible!" -ForegroundColor Red
    Write-Host "Please start MongoDB:" -ForegroundColor Yellow
    Write-Host "  Start-Service MongoDB" -ForegroundColor Cyan
    Write-Host "  Or check if MongoDB is running on port 27017" -ForegroundColor Cyan
    exit 1
}
Write-Host ""

# Step 2: Check Kafka
Write-Host "Step 2: Checking Kafka..." -ForegroundColor Yellow
$kafkaHome = "C:\Kafka\kafka_2.13-4.1.0"
if (-not (Test-Path $kafkaHome)) {
    Write-Host "ERROR: Kafka directory not found at: $kafkaHome" -ForegroundColor Red
    Write-Host "Please update the kafkaHome variable in this script" -ForegroundColor Yellow
    exit 1
}

try {
    Push-Location $kafkaHome
    $topics = & .\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 2>&1
    Pop-Location
    
    if ($LASTEXITCODE -eq 0 -and -not ($topics -match "Connection refused" -or $topics -match "Could not connect")) {
        Write-Host "SUCCESS: Kafka is running" -ForegroundColor Green
        
        # Check if audit-logs topic exists
        if ($topics -match "audit-logs") {
            Write-Host "SUCCESS: audit-logs topic exists" -ForegroundColor Green
        } else {
            Write-Host "WARNING: audit-logs topic not found (will be created automatically)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "ERROR: Kafka is not running or not accessible!" -ForegroundColor Red
        Write-Host "Please start Kafka:" -ForegroundColor Yellow
        Write-Host "  .\start-kafka-kraft.ps1" -ForegroundColor Cyan
        exit 1
    }
} catch {
    Write-Host "ERROR: Could not check Kafka!" -ForegroundColor Red
    Write-Host "Please start Kafka:" -ForegroundColor Yellow
    Write-Host "  .\start-kafka-kraft.ps1" -ForegroundColor Cyan
    exit 1
}
Write-Host ""

# Step 3: Check Spring Boot Application
Write-Host "Step 3: Checking Spring Boot Application..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/audit-logs" -Method Get -ErrorAction SilentlyContinue -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "SUCCESS: Spring Boot application is running on port 8081" -ForegroundColor Green
    }
} catch {
    Write-Host "WARNING: Spring Boot application is not running on port 8081" -ForegroundColor Yellow
    Write-Host "Please start the application:" -ForegroundColor Yellow
    Write-Host "  mvn spring-boot:run" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Press Enter after starting the application to continue..." -ForegroundColor Yellow
    Read-Host
}
Write-Host ""

# Step 4: Clear database (optional)
Write-Host "Step 4: Clear database for fresh start? (y/n)" -ForegroundColor Yellow
$clearDb = Read-Host
if ($clearDb -eq "y") {
    Write-Host "Clearing database..." -ForegroundColor Cyan
    try {
        mongosh --eval "use audit_db; db.users.deleteMany({}); db.audit_logs.deleteMany({});" 2>&1 | Out-Null
        Write-Host "SUCCESS: Database cleared" -ForegroundColor Green
    } catch {
        Write-Host "WARNING: Could not clear database" -ForegroundColor Yellow
    }
}
Write-Host ""

# Step 5: Test CREATE operation
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing CREATE Operation" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Creating a test user..." -ForegroundColor Yellow
$testUser = @{
    username = "test.user.$(Get-Date -Format 'yyyyMMddHHmmss')"
    email = "test.$(Get-Date -Format 'yyyyMMddHHmmss')@example.com"
    fullName = "Test User"
    role = "USER"
} | ConvertTo-Json

try {
    $createResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"X-Username" = "test.script"} `
        -Body $testUser
    
    Write-Host "SUCCESS: User created with ID: $($createResponse.id)" -ForegroundColor Green
    $userId = $createResponse.id
    
    Write-Host "Waiting 3 seconds for audit log processing..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
    
    # Check audit log
    $auditLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -Method Get
    $createLogs = $auditLogs | Where-Object { $_.action -eq "CREATE" -and $_.entityId -eq $userId }
    
    if ($createLogs.Count -gt 0) {
        Write-Host "SUCCESS: CREATE audit log found!" -ForegroundColor Green
        Write-Host "  Action: $($createLogs[0].action)" -ForegroundColor Cyan
        Write-Host "  Entity: $($createLogs[0].entityName)" -ForegroundColor Cyan
        Write-Host "  Username: $($createLogs[0].username)" -ForegroundColor Cyan
    } else {
        Write-Host "WARNING: CREATE audit log not found yet. Wait a few seconds and check manually." -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR: Failed to create user or check audit log" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
}
Write-Host ""

# Step 6: Test UPDATE operation
if ($userId) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Testing UPDATE Operation" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Updating user..." -ForegroundColor Yellow
    $updateUser = @{
        fullName = "Updated Test User"
        role = "ADMIN"
    } | ConvertTo-Json
    
    try {
        $updateResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/$userId" `
            -Method PUT `
            -ContentType "application/json" `
            -Headers @{"X-Username" = "test.script"} `
            -Body $updateUser
        
        Write-Host "SUCCESS: User updated" -ForegroundColor Green
        
        Write-Host "Waiting 3 seconds for audit log processing..." -ForegroundColor Yellow
        Start-Sleep -Seconds 3
        
        # Check audit log
        $auditLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -Method Get
        $updateLogs = $auditLogs | Where-Object { $_.action -eq "UPDATE" -and $_.entityId -eq $userId }
        
        if ($updateLogs.Count -gt 0) {
            Write-Host "SUCCESS: UPDATE audit log found!" -ForegroundColor Green
            Write-Host "  Action: $($updateLogs[0].action)" -ForegroundColor Cyan
            Write-Host "  Old Value: $($updateLogs[0].oldValue)" -ForegroundColor Cyan
            Write-Host "  New Value: $($updateLogs[0].newValue)" -ForegroundColor Cyan
        } else {
            Write-Host "WARNING: UPDATE audit log not found yet. Wait a few seconds and check manually." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "ERROR: Failed to update user or check audit log" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
    }
    Write-Host ""
}

# Step 7: Test DELETE operation
if ($userId) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Testing DELETE Operation" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Deleting user..." -ForegroundColor Yellow
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/$userId" `
            -Method DELETE `
            -Headers @{"X-Username" = "test.script"} | Out-Null
        
        Write-Host "SUCCESS: User deleted" -ForegroundColor Green
        
        Write-Host "Waiting 3 seconds for audit log processing..." -ForegroundColor Yellow
        Start-Sleep -Seconds 3
        
        # Check audit log
        $auditLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -Method Get
        $deleteLogs = $auditLogs | Where-Object { $_.action -eq "DELETE" -and $_.entityId -eq $userId }
        
        if ($deleteLogs.Count -gt 0) {
            Write-Host "SUCCESS: DELETE audit log found!" -ForegroundColor Green
            Write-Host "  Action: $($deleteLogs[0].action)" -ForegroundColor Cyan
            Write-Host "  Old Value: $($deleteLogs[0].oldValue)" -ForegroundColor Cyan
        } else {
            Write-Host "WARNING: DELETE audit log not found yet. Wait a few seconds and check manually." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "ERROR: Failed to delete user or check audit log" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
    }
    Write-Host ""
}

# Step 8: Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

try {
    $allLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs" -Method Get
    Write-Host "Total audit logs: $($allLogs.Count)" -ForegroundColor Cyan
    
    $createCount = ($allLogs | Where-Object { $_.action -eq "CREATE" }).Count
    $updateCount = ($allLogs | Where-Object { $_.action -eq "UPDATE" }).Count
    $deleteCount = ($allLogs | Where-Object { $_.action -eq "DELETE" }).Count
    
    Write-Host "  CREATE logs: $createCount" -ForegroundColor Green
    Write-Host "  UPDATE logs: $updateCount" -ForegroundColor Green
    Write-Host "  DELETE logs: $deleteCount" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "View all audit logs:" -ForegroundColor Yellow
    Write-Host "  http://localhost:8081/api/audit-logs" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "View in MongoDB:" -ForegroundColor Yellow
    Write-Host "  mongosh --eval 'use audit_db; db.audit_logs.find().pretty()'" -ForegroundColor Cyan
} catch {
    Write-Host "WARNING: Could not retrieve audit logs summary" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "If all operations show SUCCESS, your audit logging system is working correctly!" -ForegroundColor Green

