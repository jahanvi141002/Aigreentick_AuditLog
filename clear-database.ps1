# PowerShell script to clear the audit database and prepare for fresh testing
# This script clears all collections in the audit_db database

Write-Host "=== Clearing Audit Database ===" -ForegroundColor Cyan
Write-Host ""

# Clear MongoDB collections
Write-Host "Clearing MongoDB collections..." -ForegroundColor Yellow

try {
    # Clear users and audit_logs collections
    $result = mongosh --eval "use audit_db; db.users.deleteMany({}); db.audit_logs.deleteMany({}); print('Collections cleared successfully');" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ MongoDB collections cleared" -ForegroundColor Green
        Write-Host ""
        
        # Verify collections are empty
        Write-Host "Verifying collections are empty..." -ForegroundColor Yellow
        mongosh --eval "use audit_db; var users = db.users.countDocuments(); var logs = db.audit_logs.countDocuments(); print('Users count:', users); print('Audit logs count:', logs);" | Out-Null
        
        Write-Host ""
        Write-Host "✓ Database cleared successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "You can now test from scratch. Start by creating a new user via Postman or the API." -ForegroundColor Cyan
    } else {
        throw "MongoDB command failed"
    }
} catch {
    Write-Host "✗ Error clearing database: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please ensure:" -ForegroundColor Yellow
    Write-Host "  1. MongoDB is running on localhost:27017" -ForegroundColor Yellow
    Write-Host "  2. mongosh is installed and in your PATH" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

