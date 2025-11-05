# Test script to create 10+ users and verify batch saving
Write-Host "=== TESTING BATCH SAVING (10 records) ===" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8081/api/demo/users"
$count = 15  # Create 15 users to test batching

Write-Host "Creating $count users to test batch saving..." -ForegroundColor Yellow
Write-Host ""

for ($i = 1; $i -le $count; $i++) {
    $username = "testuser$i"
    $email = "test$i@example.com"
    
    $body = @{
        username = $username
        email = $email
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri $baseUrl -Method POST -ContentType "application/json" -Body $body
        Write-Host "[$i/$count] Created user: $username" -ForegroundColor Green
    } catch {
        Write-Host "[$i/$count] Error creating user: $username" -ForegroundColor Red
        Write-Host "   Error: $_" -ForegroundColor Red
    }
    
    # Small delay between requests
    Start-Sleep -Milliseconds 200
}

Write-Host ""
Write-Host "=== TEST COMPLETE ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Created $count users" -ForegroundColor Green
Write-Host ""
Write-Host "Check Spring Boot logs for:" -ForegroundColor Yellow
Write-Host "  - 'CONSUMER CALLED'" -ForegroundColor White
Write-Host "  - 'Collected X audit logs (>= 10), saving to database...'" -ForegroundColor White
Write-Host "  - 'Successfully saved X audit logs to database'" -ForegroundColor White
Write-Host ""
Write-Host "Expected behavior:" -ForegroundColor Cyan
Write-Host "  - Consumer should collect 10 records in batch" -ForegroundColor White
Write-Host "  - Then save all 10 records together" -ForegroundColor White
Write-Host "  - Then collect remaining 5 records and save" -ForegroundColor White
Write-Host ""
Write-Host "Check MongoDB to verify audit logs are saved:" -ForegroundColor Yellow
Write-Host "  Use MongoDB Compass or mongosh to check audit_logs collection" -ForegroundColor White

