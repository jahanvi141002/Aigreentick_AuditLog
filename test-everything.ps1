# Complete Testing Script for Audit & Exception Logging
# Run this script to test all functionality

Write-Host "=== Starting Complete Test Suite ===" -ForegroundColor Cyan
Write-Host "Make sure MongoDB and Kafka are running!" -ForegroundColor Yellow
Write-Host ""

# Test 1: Create User with Headers
Write-Host "[Test 1] Creating user with headers..." -ForegroundColor Yellow
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

try {
    $user = Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users" `
        -Method Post -Headers $headers -Body $body
    Write-Host "✅ User created: $($user.id)" -ForegroundColor Green
    $userId = $user.id
} catch {
    Write-Host "❌ Failed to create user: $_" -ForegroundColor Red
    exit 1
}

# Wait for Kafka processing
Write-Host "Waiting 3 seconds for Kafka processing..." -ForegroundColor Gray
Start-Sleep -Seconds 3

# Test 2: Verify Audit Log
Write-Host "`n[Test 2] Verifying audit log..." -ForegroundColor Yellow
try {
    $auditLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
    $latestAudit = $auditLogs[-1]

    if ($latestAudit.userId -eq "admin-123" -and $latestAudit.organizationId -eq "org-789") {
        Write-Host "✅ Audit log has correct userId and organizationId" -ForegroundColor Green
        Write-Host "   - Username: $($latestAudit.username)" -ForegroundColor Gray
        Write-Host "   - User ID: $($latestAudit.userId)" -ForegroundColor Gray
        Write-Host "   - Organization ID: $($latestAudit.organizationId)" -ForegroundColor Gray
        Write-Host "   - URL Domain: $($latestAudit.urlDomain)" -ForegroundColor Gray
    } else {
        Write-Host "❌ Audit log missing fields" -ForegroundColor Red
        Write-Host "   - User ID: $($latestAudit.userId)" -ForegroundColor Red
        Write-Host "   - Organization ID: $($latestAudit.organizationId)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Failed to get audit logs: $_" -ForegroundColor Red
}

# Test 3: Trigger Exception
Write-Host "`n[Test 3] Triggering exception..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://localhost:8081/api/demo/users/invalid-id-12345" `
        -Method Get -Headers $headers -ErrorAction Stop
    Write-Host "⚠️  No exception thrown (unexpected)" -ForegroundColor Yellow
} catch {
    Write-Host "✅ Exception triggered (expected)" -ForegroundColor Green
}

# Wait for Kafka processing
Write-Host "Waiting 3 seconds for Kafka processing..." -ForegroundColor Gray
Start-Sleep -Seconds 3

# Test 4: Verify Exception Log
Write-Host "`n[Test 4] Verifying exception log..." -ForegroundColor Yellow
try {
    $exceptionLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs"
    
    if ($exceptionLogs.Count -gt 0) {
        $latestException = $exceptionLogs[-1]
        
        if ($latestException.userId -eq "admin-123" -and $latestException.organizationId -eq "org-789") {
            Write-Host "✅ Exception log has correct userId and organizationId" -ForegroundColor Green
            Write-Host "   - Exception Type: $($latestException.exceptionType)" -ForegroundColor Gray
            Write-Host "   - User ID: $($latestException.userId)" -ForegroundColor Gray
            Write-Host "   - Organization ID: $($latestException.organizationId)" -ForegroundColor Gray
            Write-Host "   - HTTP Status: $($latestException.httpStatus)" -ForegroundColor Gray
        } else {
            Write-Host "❌ Exception log missing fields" -ForegroundColor Red
        }
    } else {
        Write-Host "⚠️  No exception logs found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Failed to get exception logs: $_" -ForegroundColor Red
}

# Test 5: Query by User ID
Write-Host "`n[Test 5] Querying audit logs by user ID..." -ForegroundColor Yellow
try {
    $userLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/user-id/admin-123"
    Write-Host "✅ Found $($userLogs.Count) audit logs for user ID" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to query by user ID: $_" -ForegroundColor Red
}

# Test 6: Query by Organization ID
Write-Host "`n[Test 6] Querying audit logs by organization ID..." -ForegroundColor Yellow
try {
    $orgLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs/organization-id/org-789"
    Write-Host "✅ Found $($orgLogs.Count) audit logs for organization ID" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to query by organization ID: $_" -ForegroundColor Red
}

# Test 7: Query Exception Logs by User ID
Write-Host "`n[Test 7] Querying exception logs by user ID..." -ForegroundColor Yellow
try {
    $exceptionLogsByUser = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs/user-id/admin-123"
    Write-Host "✅ Found $($exceptionLogsByUser.Count) exception logs for user ID" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to query exception logs by user ID: $_" -ForegroundColor Red
}

# Test 8: Query Exception Logs by Organization ID
Write-Host "`n[Test 8] Querying exception logs by organization ID..." -ForegroundColor Yellow
try {
    $exceptionLogsByOrg = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs/organization-id/org-789"
    Write-Host "✅ Found $($exceptionLogsByOrg.Count) exception logs for organization ID" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to query exception logs by organization ID: $_" -ForegroundColor Red
}

# Summary
Write-Host "`n=== Test Summary ===" -ForegroundColor Cyan
try {
    $allAuditLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
    $allExceptionLogs = Invoke-RestMethod -Uri "http://localhost:8081/api/exception-logs"
    
    Write-Host "Total Audit Logs: $($allAuditLogs.Count)" -ForegroundColor White
    Write-Host "Total Exception Logs: $($allExceptionLogs.Count)" -ForegroundColor White
    
    # Count by action
    $createCount = ($allAuditLogs | Where-Object { $_.action -eq "CREATE" }).Count
    $updateCount = ($allAuditLogs | Where-Object { $_.action -eq "UPDATE" }).Count
    $deleteCount = ($allAuditLogs | Where-Object { $_.action -eq "DELETE" }).Count
    
    Write-Host "`nAudit Logs by Action:" -ForegroundColor White
    Write-Host "  - CREATE: $createCount" -ForegroundColor Green
    Write-Host "  - UPDATE: $updateCount" -ForegroundColor Yellow
    Write-Host "  - DELETE: $deleteCount" -ForegroundColor Red
} catch {
    Write-Host "⚠️  Could not get summary: $_" -ForegroundColor Yellow
}

Write-Host "`n✅ All tests completed!" -ForegroundColor Green
Write-Host "Check the application logs for Kafka processing details." -ForegroundColor Gray
