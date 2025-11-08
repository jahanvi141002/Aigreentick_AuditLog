# Complete Testing Guide - Audit & Exception Logging

This comprehensive guide covers testing all features including:
- ✅ Audit Logging with Kafka
- ✅ Exception Logging with Kafka
- ✅ New Audit Log Fields (userId, organizationId, urlDomain)
- ✅ All REST API Endpoints

---

## 📋 Prerequisites

### 1. Start Required Services

**MongoDB:**
```powershell
# Windows
Start-Service MongoDB

# Verify
mongosh --eval "db.version()"
```

**Kafka (KRaft Mode):**
```powershell
# Windows
.\start-kafka-kraft.ps1

# Verify
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

### 2. Start Spring Boot Application

```powershell
mvn spring-boot:run
```

**Wait for these messages:**
```
✅ Started AuditApplication
✅ AuditLogKafkaConsumer initialized
✅ ExceptionLogKafkaConsumer initialized
```

---

## 🧪 Part 1: Testing Audit Logging

### Test 1.1: Create User with New Headers

**PowerShell:**
```powershell
$body = @{
    username = "john.doe"
    email = "john.doe@example.com"
    fullName = "John Doe"
    role = "USER"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
    "X-Username" = "test.user"
    "X-User-Id" = "user-123"
    "X-Organization-Id" = "org-456"
}

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
    -Method Post `
    -Headers $headers `
    -Body $body

Write-Host "Created User ID: $($response.id)" -ForegroundColor Green
$userId = $response.id
```

**cURL:**
```bash
curl -X POST http://localhost:8081/api/demo/users \
  -H "Content-Type: application/json" \
  -H "X-Username: test.user" \
  -H "X-User-Id: user-123" \
  -H "X-Organization-Id: org-456" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "role": "USER"
  }'
```

### Test 1.2: Verify Audit Log with New Fields

**Wait 2-3 seconds for Kafka processing, then:**

```powershell
# Get all audit logs
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
$latestLog = $logs[-1]

Write-Host "`n=== Audit Log Details ===" -ForegroundColor Cyan
Write-Host "Username: $($latestLog.username)" -ForegroundColor White
Write-Host "User ID: $($latestLog.userId)" -ForegroundColor White
Write-Host "Organization ID: $($latestLog.organizationId)" -ForegroundColor White
Write-Host "URL Domain: $($latestLog.urlDomain)" -ForegroundColor White
Write-Host "Action: $($latestLog.action)" -ForegroundColor White
Write-Host "Entity: $($latestLog.entityName)" -ForegroundColor White
```

**Expected Output:**
```
Username: test.user
User ID: user-123
Organization ID: org-456
URL Domain: localhost
Action: CREATE
Entity: User
```

### Test 1.3: Query Audit Logs by User ID

```powershell
$userId = "user-123"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/user-id/$userId"
Write-Host "Audit logs for User ID $userId : $($logs.Count)" -ForegroundColor Cyan
```

### Test 1.4: Query Audit Logs by Organization ID

```powershell
$orgId = "org-456"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/organization-id/$orgId"
Write-Host "Audit logs for Organization ID $orgId : $($logs.Count)" -ForegroundColor Cyan
```

### Test 1.5: Query Audit Logs by URL Domain

```powershell
$domain = "localhost"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/url-domain/$domain"
Write-Host "Audit logs for domain $domain : $($logs.Count)" -ForegroundColor Cyan
```

### Test 1.6: Update User (Test UPDATE Audit Log)

```powershell
$updateBody = @{
    username = "john.doe"
    email = "john.updated@example.com"
    fullName = "John Doe Updated"
    role = "ADMIN"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
    "X-Username" = "test.user"
    "X-User-Id" = "user-123"
    "X-Organization-Id" = "org-456"
}

Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/$userId" `
    -Method Put `
    -Headers $headers `
    -Body $updateBody | Out-Null

Write-Host "User updated" -ForegroundColor Green

# Wait and verify
Start-Sleep -Seconds 3
$updateLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/UPDATE"
Write-Host "UPDATE audit logs: $($updateLogs.Count)" -ForegroundColor Yellow
```

### Test 1.7: Delete User (Test DELETE Audit Log)

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/$userId" `
    -Method Delete `
    -Headers @{"X-Username"="test.user"; "X-User-Id"="user-123"; "X-Organization-Id"="org-456"} | Out-Null

Write-Host "User deleted" -ForegroundColor Green

# Wait and verify
Start-Sleep -Seconds 3
$deleteLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/DELETE"
Write-Host "DELETE audit logs: $($deleteLogs.Count)" -ForegroundColor Red
```

---

## 🧪 Part 2: Testing Exception Logging

### Test 2.1: Trigger an Exception (IllegalArgumentException)

```powershell
# This endpoint should throw an exception
try {
    Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/invalid-id" `
        -Method Get `
        -Headers @{
            "X-Username" = "test.user"
            "X-User-Id" = "user-123"
            "X-Organization-Id" = "org-456"
        } -ErrorAction Stop
} catch {
    Write-Host "Exception triggered (expected)" -ForegroundColor Yellow
}
```

### Test 2.2: Verify Exception Log

**Wait 2-3 seconds for Kafka processing, then:**

```powershell
# Get all exception logs
$exceptionLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs"
$latestException = $exceptionLogs[-1]

Write-Host "`n=== Exception Log Details ===" -ForegroundColor Red
Write-Host "Exception Type: $($latestException.exceptionType)" -ForegroundColor White
Write-Host "Exception Message: $($latestException.exceptionMessage)" -ForegroundColor White
Write-Host "Username: $($latestException.username)" -ForegroundColor White
Write-Host "User ID: $($latestException.userId)" -ForegroundColor White
Write-Host "Organization ID: $($latestException.organizationId)" -ForegroundColor White
Write-Host "Request URL: $($latestException.requestUrl)" -ForegroundColor White
Write-Host "HTTP Status: $($latestException.httpStatus)" -ForegroundColor White
Write-Host "Class Name: $($latestException.className)" -ForegroundColor White
```

### Test 2.3: Query Exception Logs by Exception Type

```powershell
$exceptionType = "java.lang.IllegalArgumentException"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs/type/$exceptionType"
Write-Host "Exception logs for type $exceptionType : $($logs.Count)" -ForegroundColor Cyan
```

### Test 2.4: Query Exception Logs by User ID

```powershell
$userId = "user-123"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs/user-id/$userId"
Write-Host "Exception logs for User ID $userId : $($logs.Count)" -ForegroundColor Cyan
```

### Test 2.5: Query Exception Logs by Organization ID

```powershell
$orgId = "org-456"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs/organization-id/$orgId"
Write-Host "Exception logs for Organization ID $orgId : $($logs.Count)" -ForegroundColor Cyan
```

### Test 2.6: Query Exception Logs by HTTP Status

```powershell
$status = 400
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs/http-status/$status"
Write-Host "Exception logs with HTTP status $status : $($logs.Count)" -ForegroundColor Cyan
```

### Test 2.7: View Full Stack Trace

```powershell
$exceptionLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs"
$latestException = $exceptionLogs[-1]

Write-Host "`n=== Full Stack Trace ===" -ForegroundColor Yellow
Write-Host $latestException.stackTrace
```

---

## 🧪 Part 3: Testing All REST API Endpoints

### Audit Log Endpoints

```powershell
$baseUrl = "http://localhost:8081/api/audit-logs"

# Get all audit logs
Invoke-RestMethod -Uri "$baseUrl"

# Get by ID
$id = "YOUR_AUDIT_LOG_ID"
Invoke-RestMethod -Uri "$baseUrl/$id"

# Get by username
Invoke-RestMethod -Uri "$baseUrl/username/test.user"

# Get by user ID
Invoke-RestMethod -Uri "$baseUrl/user-id/user-123"

# Get by organization ID
Invoke-RestMethod -Uri "$baseUrl/organization-id/org-456"

# Get by URL domain
Invoke-RestMethod -Uri "$baseUrl/url-domain/localhost"

# Get by entity name
Invoke-RestMethod -Uri "$baseUrl/entity/User"

# Get by action
Invoke-RestMethod -Uri "$baseUrl/action/CREATE"

# Get by entity name and ID
Invoke-RestMethod -Uri "$baseUrl/entity/User/YOUR_ENTITY_ID"

# Get by date range
$start = "2024-01-01T00:00:00"
$end = "2024-12-31T23:59:59"
Invoke-RestMethod -Uri "$baseUrl/date-range?start=$start&end=$end"

# Get by username and date range
Invoke-RestMethod -Uri "$baseUrl/username/test.user/date-range?start=$start&end=$end"

# Get by user ID and date range
Invoke-RestMethod -Uri "$baseUrl/user-id/user-123/date-range?start=$start&end=$end"

# Get by organization ID and date range
Invoke-RestMethod -Uri "$baseUrl/organization-id/org-456/date-range?start=$start&end=$end"

# Get by URL domain and date range
Invoke-RestMethod -Uri "$baseUrl/url-domain/localhost/date-range?start=$start&end=$end"
```

### Exception Log Endpoints

```powershell
$baseUrl = "http://localhost:8081/api/exception-logs"

# Get all exception logs
Invoke-RestMethod -Uri "$baseUrl"

# Get by ID
$id = "YOUR_EXCEPTION_LOG_ID"
Invoke-RestMethod -Uri "$baseUrl/$id"

# Get by exception type
Invoke-RestMethod -Uri "$baseUrl/type/java.lang.IllegalArgumentException"

# Get by username
Invoke-RestMethod -Uri "$baseUrl/username/test.user"

# Get by user ID
Invoke-RestMethod -Uri "$baseUrl/user-id/user-123"

# Get by organization ID
Invoke-RestMethod -Uri "$baseUrl/organization-id/org-456"

# Get by class name
Invoke-RestMethod -Uri "$baseUrl/class/com.aigreentick.audit.controller.DatabaseAuditDemoController"

# Get by HTTP status
Invoke-RestMethod -Uri "$baseUrl/http-status/400"

# Get by date range
$start = "2024-01-01T00:00:00"
$end = "2024-12-31T23:59:59"
Invoke-RestMethod -Uri "$baseUrl/date-range?start=$start&end=$end"

# Get by exception type and date range
Invoke-RestMethod -Uri "$baseUrl/type/java.lang.IllegalArgumentException/date-range?start=$start&end=$end"

# Get by username and date range
Invoke-RestMethod -Uri "$baseUrl/username/test.user/date-range?start=$start&end=$end"

# Get by organization ID and date range
Invoke-RestMethod -Uri "$baseUrl/organization-id/org-456/date-range?start=$start&end=$end"
```

---

## 🧪 Part 4: Testing Kafka Integration

### Test 4.1: Verify Kafka Topics

```powershell
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

**Expected Topics:**
- `audit-logs`
- `exception-logs`
- `__consumer_offsets` (internal)

### Test 4.2: Monitor Kafka Messages (Audit Logs)

```powershell
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 `
    --topic audit-logs `
    --from-beginning
```

### Test 4.3: Monitor Kafka Messages (Exception Logs)

```powershell
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 `
    --topic exception-logs `
    --from-beginning
```

### Test 4.4: Test Batch Processing

**Create multiple users to test batching:**

```powershell
# Create 15 users (batch size is 10)
1..15 | ForEach-Object {
    $body = @{
        username = "batch.user$_"
        email = "batch$_@example.com"
        fullName = "Batch User $_"
        role = "USER"
    } | ConvertTo-Json

    Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
        -Method Post `
        -Headers @{
            "Content-Type" = "application/json"
            "X-Username" = "batch.user"
            "X-User-Id" = "batch-user-123"
            "X-Organization-Id" = "batch-org-456"
        } `
        -Body $body | Out-Null
}

Write-Host "Created 15 users" -ForegroundColor Green

# Wait for batch processing
Start-Sleep -Seconds 5

# Check audit logs
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/CREATE"
Write-Host "CREATE audit logs: $($logs.Count)" -ForegroundColor Cyan
```

**Check Application Logs** - You should see:
```
✅ Received batch of 10 audit logs from topic audit-logs
✅ Saved 10 audit logs to database
✅ Received batch of 5 audit logs from topic audit-logs
✅ Saved 5 audit logs to database
```

---

## 🧪 Part 5: Testing Exception Scenarios

### Test 5.1: Trigger NullPointerException

```powershell
# Create a test endpoint that throws NullPointerException
# Or test with invalid data that causes NPE
```

### Test 5.2: Trigger RuntimeException

```powershell
# Test with operations that cause runtime exceptions
```

### Test 5.3: Verify Exception Logging in MongoDB

```powershell
mongosh --quiet --eval "use audit_db; db.exception_logs.find().pretty()"
```

---

## 🧪 Part 6: Complete End-to-End Test Script

**Save as `test-everything.ps1`:**

```powershell
# Complete Testing Script
Write-Host "=== Starting Complete Test Suite ===" -ForegroundColor Cyan

# Test 1: Create User with Headers
Write-Host "`n[Test 1] Creating user with headers..." -ForegroundColor Yellow
$body = @{
    username = "test.user"
    email = "test@example.com"
    fullName = "Test User"
    role = "USER"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
    "X-Username" = "admin"
    "X-User-Id" = "admin-123"
    "X-Organization-Id" = "org-789"
}

$user = Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
    -Method Post -Headers $headers -Body $body
Write-Host "✅ User created: $($user.id)" -ForegroundColor Green

# Wait for Kafka processing
Start-Sleep -Seconds 3

# Test 2: Verify Audit Log
Write-Host "`n[Test 2] Verifying audit log..." -ForegroundColor Yellow
$auditLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
$latestAudit = $auditLogs[-1]

if ($latestAudit.userId -eq "admin-123" -and $latestAudit.organizationId -eq "org-789") {
    Write-Host "✅ Audit log has correct userId and organizationId" -ForegroundColor Green
} else {
    Write-Host "❌ Audit log missing fields" -ForegroundColor Red
}

# Test 3: Trigger Exception
Write-Host "`n[Test 3] Triggering exception..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/invalid" `
        -Method Get -Headers $headers -ErrorAction Stop
} catch {
    Write-Host "✅ Exception triggered" -ForegroundColor Green
}

# Wait for Kafka processing
Start-Sleep -Seconds 3

# Test 4: Verify Exception Log
Write-Host "`n[Test 4] Verifying exception log..." -ForegroundColor Yellow
$exceptionLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs"
$latestException = $exceptionLogs[-1]

if ($latestException.userId -eq "admin-123" -and $latestException.organizationId -eq "org-789") {
    Write-Host "✅ Exception log has correct userId and organizationId" -ForegroundColor Green
} else {
    Write-Host "❌ Exception log missing fields" -ForegroundColor Red
}

# Test 5: Query by User ID
Write-Host "`n[Test 5] Querying audit logs by user ID..." -ForegroundColor Yellow
$userLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/user-id/admin-123"
Write-Host "✅ Found $($userLogs.Count) audit logs for user ID" -ForegroundColor Green

# Test 6: Query by Organization ID
Write-Host "`n[Test 6] Querying audit logs by organization ID..." -ForegroundColor Yellow
$orgLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/organization-id/org-789"
Write-Host "✅ Found $($orgLogs.Count) audit logs for organization ID" -ForegroundColor Green

# Summary
Write-Host "`n=== Test Summary ===" -ForegroundColor Cyan
Write-Host "Total Audit Logs: $($auditLogs.Count)" -ForegroundColor White
Write-Host "Total Exception Logs: $($exceptionLogs.Count)" -ForegroundColor White
Write-Host "`n✅ All tests completed!" -ForegroundColor Green
```

**Run the script:**
```powershell
.\test-everything.ps1
```

---

## ✅ Success Criteria

All tests pass if:

1. ✅ **Audit logs are created** with userId, organizationId, urlDomain
2. ✅ **Exception logs are created** with userId, organizationId
3. ✅ **Kafka topics exist** (audit-logs, exception-logs)
4. ✅ **Batch processing works** (logs are batched before saving)
5. ✅ **All REST endpoints work** (no 404 errors)
6. ✅ **Queries by new fields work** (userId, organizationId, urlDomain)
7. ✅ **MongoDB contains logs** (audit_logs and exception_logs collections)

---

## 🐛 Troubleshooting

### Issue: Headers not captured

**Check:**
- Headers are sent correctly (X-User-Id, X-Organization-Id)
- MongoAuditWebFilter is running (check logs)
- Request is not bypassing the filter

### Issue: Exception logs not appearing

**Check:**
- Exception is actually thrown
- GlobalExceptionHandler is catching it
- Kafka consumer is running
- Wait 2-3 seconds for processing

### Issue: Kafka not processing

**Check:**
- Kafka is running
- Topics exist
- Consumer group is active
- Check application logs for errors

---

## 📝 Summary

This guide covers:
- ✅ Audit logging with new fields
- ✅ Exception logging
- ✅ Kafka integration
- ✅ All REST API endpoints
- ✅ Batch processing
- ✅ Query capabilities

**Run the complete test script to verify everything works!** 🎉

