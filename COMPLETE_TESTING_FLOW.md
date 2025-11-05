# Complete Testing Flow for Audit Logging

This guide provides a complete, step-by-step flow to test the audit logging system from start to finish.

---

## 🎯 Overview

The audit logging system automatically captures all database operations (CREATE, UPDATE, DELETE) and sends them to Kafka for batched processing. This guide will walk you through:

1. **Prerequisites Setup** - Verify and start required services
2. **Application Startup** - Start the Spring Boot application
3. **Testing Scenarios** - Test CREATE, UPDATE, DELETE operations
4. **Verification** - Verify audit logs are created correctly

---

## 📋 Phase 1: Prerequisites Setup

### Step 1.1: Check MongoDB is Running

**Windows PowerShell:**
```powershell
# Check MongoDB connection
mongosh --eval "db.version()"
```

**Expected Output:**
```
MongoDB shell version 7.x.x
```

**If MongoDB is not running:**
```powershell
# Start MongoDB service (Windows)
Start-Service MongoDB

# Or check status
Get-Service MongoDB
```

**macOS/Linux:**
```bash
# Check MongoDB
mongosh --eval "db.version()"

# Start MongoDB (if needed)
brew services start mongodb-community  # macOS
# OR
sudo systemctl start mongod  # Linux
```

---

### Step 1.2: Start Kafka in KRaft Mode

**Windows PowerShell:**
```powershell
# Option 1: Use the provided script
.\start-kafka-kraft.ps1

# Option 2: Manual start
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-server-start.bat .\config\kraft\server.properties
```

**macOS/Linux:**
```bash
cd /path/to/kafka
bin/kafka-server-start.sh config/kraft/server.properties
```

**Verify Kafka is Running:**
```powershell
# Windows
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

# macOS/Linux
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

**Expected Output:** Should list topics (may be empty initially, or show `__consumer_offsets`)

---

### Step 1.3: Clear Database (Optional - Fresh Start)

**Windows PowerShell:**
```powershell
.\clear-database.ps1
```

**macOS/Linux:**
```bash
mongosh --eval "use audit_db; db.users.deleteMany({}); db.audit_logs.deleteMany({});"
```

**Verify Database is Empty:**
```powershell
mongosh --quiet --eval "use audit_db; print('Users:', db.users.countDocuments()); print('Audit Logs:', db.audit_logs.countDocuments());"
```

**Expected Output:**
```
Users: 0
Audit Logs: 0
```

---

## 🚀 Phase 2: Start Spring Boot Application

### Step 2.1: Build the Application

```powershell
# In project directory
mvn clean install
```

**Expected Output:** `BUILD SUCCESS`

---

### Step 2.2: Start the Application

```powershell
mvn spring-boot:run
```

**Wait for Application to Start** - Look for these messages in the console:

```
✅ Started AuditApplication in X.XXX seconds
✅ Kafka consumer started
✅ Connected to Kafka broker
```

**Common Issues:**
- ❌ `Connection refused` → Kafka is not running (go back to Step 1.2)
- ❌ `MongoDB connection failed` → MongoDB is not running (go back to Step 1.1)

---

### Step 2.3: Verify Application is Running

**Open a new terminal/PowerShell window** and test:

```powershell
# Test API endpoint
Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
```

**Expected Response:** `[]` (empty array)

**Or using curl:**
```bash
curl http://localhost:8081/api/audit-logs
```

**Expected Response:** `[]`

---

## 🧪 Phase 3: Testing Scenarios

### Test 1: Create a User (CREATE Audit Log)

**Purpose:** Test that CREATE operations generate audit logs.

**Using PowerShell:**
```powershell
$body = @{
    username = "john.doe"
    email = "john.doe@example.com"
    fullName = "John Doe"
    role = "USER"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
    -Method Post `
    -Headers @{"Content-Type"="application/json"; "X-Username"="test.user"} `
    -Body $body

Write-Host "Created User ID: $($response.id)" -ForegroundColor Green
```

**Using Postman:**
- **Method:** `POST`
- **URL:** `http://localhost:8081/api/demo/users`
- **Headers:**
  ```
  Content-Type: application/json
  X-Username: test.user
  ```
- **Body (raw JSON):**
  ```json
  {
    "username": "john.doe",
    "email": "john.doe@example.com",
    "fullName": "John Doe",
    "role": "USER"
  }
  ```

**Expected Response:** `201 Created` with user object including `id`

**Save the `id` from the response** - you'll need it for UPDATE and DELETE tests.

---

### Test 2: Verify CREATE Audit Log (Wait 2-3 seconds)

**Wait 2-3 seconds** for Kafka to process the audit log, then verify:

```powershell
# Get all audit logs
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
Write-Host "Total audit logs: $($logs.Count)" -ForegroundColor Cyan

# Get CREATE audit logs only
$createLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/CREATE"
Write-Host "CREATE audit logs: $($createLogs.Count)" -ForegroundColor Green

# Display the audit log
if ($createLogs.Count -gt 0) {
    $createLogs[0] | ConvertTo-Json -Depth 5
}
```

**Expected Output:**
```json
{
  "id": "...",
  "username": "test.user",
  "entityName": "User",
  "entityId": "...",
  "action": "CREATE",
  "oldValue": null,
  "newValue": "{\"id\":\"...\",\"username\":\"john.doe\",\"email\":\"john.doe@example.com\",\"fullName\":\"John Doe\",\"role\":\"USER\"}",
  "timestamp": "2024-01-15T10:30:00",
  "ipAddress": "127.0.0.1"
}
```

**Check Application Logs** - You should see:
```
✅ Audit log sent to Kafka for CREATE operation on User
✅ Received batch of 1 audit logs from topic audit-logs
✅ Saved 1 audit logs to database
```

---

### Test 3: Update a User (UPDATE Audit Log)

**Purpose:** Test that UPDATE operations generate audit logs with old and new values.

**Using PowerShell:**
```powershell
# Replace USER_ID with the ID from Test 1
$userId = "YOUR_USER_ID_HERE"

$updateBody = @{
    username = "john.doe"
    email = "john.doe.updated@example.com"
    fullName = "John Doe Updated"
    role = "ADMIN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/$userId" `
    -Method Put `
    -Headers @{"Content-Type"="application/json"; "X-Username"="test.user"} `
    -Body $updateBody | Out-Null

Write-Host "User updated successfully" -ForegroundColor Green
```

**Using Postman:**
- **Method:** `PUT`
- **URL:** `http://localhost:8081/api/demo/users/{USER_ID}` (replace {USER_ID})
- **Headers:**
  ```
  Content-Type: application/json
  X-Username: test.user
  ```
- **Body (raw JSON):**
  ```json
  {
    "username": "john.doe",
    "email": "john.doe.updated@example.com",
    "fullName": "John Doe Updated",
    "role": "ADMIN"
  }
  ```

**Expected Response:** `200 OK` with updated user object

---

### Test 4: Verify UPDATE Audit Log (Wait 2-3 seconds)

**Wait 2-3 seconds** for Kafka to process, then verify:

```powershell
# Get UPDATE audit logs
$updateLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/UPDATE"
Write-Host "UPDATE audit logs: $($updateLogs.Count)" -ForegroundColor Green

# Display the audit log
if ($updateLogs.Count -gt 0) {
    $updateLogs[0] | ConvertTo-Json -Depth 5
}
```

**Expected Output:**
```json
{
  "id": "...",
  "username": "test.user",
  "entityName": "User",
  "entityId": "...",
  "action": "UPDATE",
  "oldValue": "{\"id\":\"...\",\"username\":\"john.doe\",\"email\":\"john.doe@example.com\",\"fullName\":\"John Doe\",\"role\":\"USER\"}",
  "newValue": "{\"id\":\"...\",\"username\":\"john.doe\",\"email\":\"john.doe.updated@example.com\",\"fullName\":\"John Doe Updated\",\"role\":\"ADMIN\"}",
  "timestamp": "2024-01-15T10:35:00",
  "ipAddress": "127.0.0.1"
}
```

**Key Points:**
- ✅ `oldValue` contains the previous state
- ✅ `newValue` contains the updated state
- ✅ Both old and new values are captured automatically!

---

### Test 5: Delete a User (DELETE Audit Log)

**Purpose:** Test that DELETE operations generate audit logs with deleted document.

**Using PowerShell:**
```powershell
# Replace USER_ID with the ID from Test 1
$userId = "YOUR_USER_ID_HERE"

Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/$userId" `
    -Method Delete `
    -Headers @{"X-Username"="test.user"} | Out-Null

Write-Host "User deleted successfully" -ForegroundColor Green
```

**Using Postman:**
- **Method:** `DELETE`
- **URL:** `http://localhost:8081/api/demo/users/{USER_ID}` (replace {USER_ID})
- **Headers:**
  ```
  X-Username: test.user
  ```

**Expected Response:** `204 No Content`

---

### Test 6: Verify DELETE Audit Log (Wait 2-3 seconds)

**Wait 2-3 seconds** for Kafka to process, then verify:

```powershell
# Get DELETE audit logs
$deleteLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/DELETE"
Write-Host "DELETE audit logs: $($deleteLogs.Count)" -ForegroundColor Green

# Display the audit log
if ($deleteLogs.Count -gt 0) {
    $deleteLogs[0] | ConvertTo-Json -Depth 5
}
```

**Expected Output:**
```json
{
  "id": "...",
  "username": "test.user",
  "entityName": "User",
  "entityId": "...",
  "action": "DELETE",
  "oldValue": "{\"id\":\"...\",\"username\":\"john.doe\",\"email\":\"john.doe.updated@example.com\",\"fullName\":\"John Doe Updated\",\"role\":\"ADMIN\"}",
  "newValue": null,
  "timestamp": "2024-01-15T10:40:00",
  "ipAddress": "127.0.0.1"
}
```

**Key Points:**
- ✅ `oldValue` contains the deleted document
- ✅ `newValue` is null (document was deleted)
- ✅ Deleted document is captured automatically!

---

## ✅ Phase 4: Verification and Summary

### Step 4.1: Get All Audit Logs

```powershell
# Get all audit logs
$allLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "Total audit logs: $($allLogs.Count)" -ForegroundColor White

# Count by action
$createCount = ($allLogs | Where-Object { $_.action -eq "CREATE" }).Count
$updateCount = ($allLogs | Where-Object { $_.action -eq "UPDATE" }).Count
$deleteCount = ($allLogs | Where-Object { $_.action -eq "DELETE" }).Count

Write-Host "CREATE logs: $createCount" -ForegroundColor Green
Write-Host "UPDATE logs: $updateCount" -ForegroundColor Yellow
Write-Host "DELETE logs: $deleteCount" -ForegroundColor Red
```

**Expected Output:**
```
=== Summary ===
Total audit logs: 3
CREATE logs: 1
UPDATE logs: 1
DELETE logs: 1
```

---

### Step 4.2: Verify in MongoDB

**Check MongoDB directly:**
```powershell
mongosh --quiet --eval "use audit_db; print('Users:', db.users.countDocuments()); print('Audit Logs:', db.audit_logs.countDocuments());"
```

**View audit logs in MongoDB:**
```powershell
mongosh --quiet --eval "use audit_db; db.audit_logs.find().pretty()"
```

---

### Step 4.3: Test Batch Processing (Optional)

**Create multiple users to test Kafka batch processing:**

```powershell
# Create 10 users quickly
1..10 | ForEach-Object {
    $body = @{
        username = "user$_"
        email = "user$_@example.com"
        fullName = "User $_"
        role = "USER"
    } | ConvertTo-Json

    Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"; "X-Username"="batch.user"} `
        -Body $body | Out-Null
}

Write-Host "Created 10 users" -ForegroundColor Green

# Wait 5 seconds for batch processing
Start-Sleep -Seconds 5

# Check audit logs
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/action/CREATE"
Write-Host "CREATE audit logs: $($logs.Count)" -ForegroundColor Cyan
```

**Check Application Logs** - You should see:
```
✅ Received batch of 10 audit logs from topic audit-logs
✅ Saved 10 audit logs to database
```

---

## 🔍 Phase 5: Advanced Queries

### Query Audit Logs by Username

```powershell
$username = "test.user"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/username/$username"
Write-Host "Audit logs for $username : $($logs.Count)" -ForegroundColor Cyan
```

---

### Query Audit Logs by Entity Type

```powershell
$entityName = "User"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/entity/$entityName"
Write-Host "Audit logs for $entityName : $($logs.Count)" -ForegroundColor Cyan
```

---

### Query Audit Logs by Entity ID

```powershell
$entityId = "YOUR_ENTITY_ID_HERE"
$logs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/entity/User/$entityId"
Write-Host "Audit logs for entity $entityId : $($logs.Count)" -ForegroundColor Cyan
```

---

## 🎯 Success Criteria

✅ **All tests should pass if:**

1. **MongoDB is running** - Users and audit logs are stored
2. **Kafka is running in KRaft mode** - Audit logs are sent to Kafka
3. **Spring Boot application is running** - API endpoints are accessible
4. **Audit logs are created automatically** - No manual logging needed
5. **Batch processing works** - Multiple logs are batched together
6. **Old and new values captured** - UPDATE operations show before/after states
7. **Deleted documents captured** - DELETE operations preserve deleted data

---

## 🐛 Troubleshooting

### Issue: Audit logs not appearing

**Check:**
1. ✅ Kafka is running (Step 1.2)
2. ✅ Application logs show "Kafka consumer started"
3. ✅ Wait 2-3 seconds after operations
4. ✅ Check application logs for errors

**Run troubleshooting script:**
```powershell
.\troubleshoot-audit-logs.ps1
```

---

### Issue: Kafka connection failed

**Check:**
1. ✅ Kafka is running on port 9092
2. ✅ Kafka is in KRaft mode (not Zookeeper mode)
3. ✅ Storage is formatted (first time setup)

**Fix Kafka:**
```powershell
.\fix-kafka-stray-logs.ps1
.\start-kafka-kraft.ps1
```

---

### Issue: MongoDB connection failed

**Check:**
1. ✅ MongoDB is running on port 27017
2. ✅ MongoDB service is started
3. ✅ Connection string in `application.properties` is correct

**Start MongoDB:**
```powershell
Start-Service MongoDB
```

---

## 📝 Summary

This complete flow demonstrates:

1. ✅ **Automatic Audit Logging** - No manual code needed
2. ✅ **Kafka Integration** - Async processing with batching
3. ✅ **Database-Level Capture** - All operations are audited
4. ✅ **Before/After Values** - UPDATE operations capture changes
5. ✅ **Deleted Document Preservation** - DELETE operations preserve data
6. ✅ **KRaft Mode** - Modern Kafka setup without Zookeeper

**The audit logging system is working correctly if all tests pass!** 🎉

