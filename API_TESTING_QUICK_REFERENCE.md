# API Testing Quick Reference

Quick reference for testing all endpoints using Postman, cURL, or PowerShell.

---

## 🔧 Setup Headers

**Required Headers for Audit Logging:**
```
X-Username: test.user
X-User-Id: user-123
X-Organization-Id: org-456
```

---

## 📝 Audit Log Endpoints

### 1. Create Audit Log (Manual)
```
POST http://localhost:8081/api/audit-logs
Headers: Content-Type: application/json
Body:
{
  "username": "test.user",
  "userId": "user-123",
  "organizationId": "org-456",
  "urlDomain": "localhost",
  "entityName": "User",
  "entityId": "entity-123",
  "action": "CREATE",
  "newValue": "{\"id\":\"123\"}"
}
```

### 2. Get All Audit Logs
```
GET http://localhost:8081/api/audit-logs
```

### 3. Get Audit Log by ID
```
GET http://localhost:8081/api/audit-logs/{id}
```

### 4. Get by Username
```
GET http://localhost:8081/api/audit-logs/username/{username}
```

### 5. Get by User ID
```
GET http://localhost:8081/api/audit-logs/user-id/{userId}
```

### 6. Get by Organization ID
```
GET http://localhost:8081/api/audit-logs/organization-id/{organizationId}
```

### 7. Get by URL Domain
```
GET http://localhost:8081/api/audit-logs/url-domain/{urlDomain}
```

### 8. Get by Entity Name
```
GET http://localhost:8081/api/audit-logs/entity/{entityName}
```

### 9. Get by Action
```
GET http://localhost:8081/api/audit-logs/action/{action}
Examples: CREATE, UPDATE, DELETE
```

### 10. Get by Entity Name and ID
```
GET http://localhost:8081/api/audit-logs/entity/{entityName}/{entityId}
```

### 11. Get by Date Range
```
GET http://localhost:8081/api/audit-logs/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 12. Get by Username and Date Range
```
GET http://localhost:8081/api/audit-logs/username/{username}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 13. Get by User ID and Date Range
```
GET http://localhost:8081/api/audit-logs/user-id/{userId}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 14. Get by Organization ID and Date Range
```
GET http://localhost:8081/api/audit-logs/organization-id/{organizationId}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 15. Get by URL Domain and Date Range
```
GET http://localhost:8081/api/audit-logs/url-domain/{urlDomain}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

---

## 🚨 Exception Log Endpoints

### 1. Create Exception Log (Manual)
```
POST http://localhost:8081/api/exception-logs
Headers: Content-Type: application/json
Body:
{
  "exceptionType": "java.lang.IllegalArgumentException",
  "exceptionMessage": "Invalid argument",
  "className": "com.example.Controller",
  "methodName": "testMethod",
  "requestUrl": "/api/test",
  "requestMethod": "GET",
  "username": "test.user",
  "userId": "user-123",
  "organizationId": "org-456",
  "ipAddress": "127.0.0.1",
  "httpStatus": 400
}
```

### 2. Get All Exception Logs
```
GET http://localhost:8081/api/exception-logs
```

### 3. Get Exception Log by ID
```
GET http://localhost:8081/api/exception-logs/{id}
```

### 4. Get by Exception Type
```
GET http://localhost:8081/api/exception-logs/type/{exceptionType}
Example: java.lang.IllegalArgumentException
```

### 5. Get by Username
```
GET http://localhost:8081/api/exception-logs/username/{username}
```

### 6. Get by User ID
```
GET http://localhost:8081/api/exception-logs/user-id/{userId}
```

### 7. Get by Organization ID
```
GET http://localhost:8081/api/exception-logs/organization-id/{organizationId}
```

### 8. Get by Class Name
```
GET http://localhost:8081/api/exception-logs/class/{className}
```

### 9. Get by HTTP Status
```
GET http://localhost:8081/api/exception-logs/http-status/{httpStatus}
Examples: 400, 500
```

### 10. Get by Date Range
```
GET http://localhost:8081/api/exception-logs/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 11. Get by Exception Type and Date Range
```
GET http://localhost:8081/api/exception-logs/type/{exceptionType}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 12. Get by Username and Date Range
```
GET http://localhost:8081/api/exception-logs/username/{username}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### 13. Get by Organization ID and Date Range
```
GET http://localhost:8081/api/exception-logs/organization-id/{organizationId}/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

---

## 🧪 Test Scenarios

### Scenario 1: Create User (Auto Audit Log)
```
POST http://localhost:8081/api/demo/users
Headers:
  Content-Type: application/json
  X-Username: test.user
  X-User-Id: user-123
  X-Organization-Id: org-456
Body:
{
  "username": "john.doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER"
}
```

**Wait 2-3 seconds, then check:**
```
GET http://localhost:8081/api/audit-logs/action/CREATE
```

### Scenario 2: Trigger Exception
```
GET http://localhost:8081/api/demo/users/invalid-id
Headers:
  X-Username: test.user
  X-User-Id: user-123
  X-Organization-Id: org-456
```

**Wait 2-3 seconds, then check:**
```
GET http://localhost:8081/api/exception-logs
```

### Scenario 3: Query by User ID
```
GET http://localhost:8081/api/audit-logs/user-id/user-123
GET http://localhost:8081/api/exception-logs/user-id/user-123
```

### Scenario 4: Query by Organization ID
```
GET http://localhost:8081/api/audit-logs/organization-id/org-456
GET http://localhost:8081/api/exception-logs/organization-id/org-456
```

---

## 📊 cURL Examples

### Create User with Headers
```bash
curl -X POST http://localhost:8081/api/demo/users \
  -H "Content-Type: application/json" \
  -H "X-Username: test.user" \
  -H "X-User-Id: user-123" \
  -H "X-Organization-Id: org-456" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "USER"
  }'
```

### Get Audit Logs by User ID
```bash
curl http://localhost:8081/api/audit-logs/user-id/user-123
```

### Get Exception Logs
```bash
curl http://localhost:8081/api/exception-logs
```

---

## 🎯 Postman Collection Setup

### Environment Variables
```
baseUrl: http://localhost:8081
username: test.user
userId: user-123
organizationId: org-456
```

### Pre-request Script (for all requests)
```javascript
pm.request.headers.add({
    key: 'X-Username',
    value: pm.environment.get('username')
});
pm.request.headers.add({
    key: 'X-User-Id',
    value: pm.environment.get('userId')
});
pm.request.headers.add({
    key: 'X-Organization-Id',
    value: pm.environment.get('organizationId')
});
```

---

## ✅ Verification Checklist

After running tests, verify:

- [ ] Audit logs are created with userId, organizationId, urlDomain
- [ ] Exception logs are created with userId, organizationId
- [ ] Kafka topics exist (audit-logs, exception-logs)
- [ ] All REST endpoints return 200 OK
- [ ] Queries by userId work
- [ ] Queries by organizationId work
- [ ] Queries by urlDomain work
- [ ] Date range queries work
- [ ] MongoDB contains audit_logs collection
- [ ] MongoDB contains exception_logs collection

---

## 🐛 Common Issues

### Headers Not Captured
- Ensure headers start with `X-` prefix
- Check MongoAuditWebFilter is running
- Verify request is not bypassing filter

### No Audit Logs
- Wait 2-3 seconds for Kafka processing
- Check Kafka is running
- Verify application logs for errors

### No Exception Logs
- Ensure exception is actually thrown
- Check GlobalExceptionHandler is active
- Wait 2-3 seconds for Kafka processing

---

## 📝 Notes

- **Kafka Processing**: Wait 2-3 seconds after operations for Kafka to process
- **Batch Size**: Default batch size is 10 records
- **Date Format**: Use ISO 8601 format: `2024-01-01T00:00:00`
- **URL Domain**: Automatically extracted from request URL hostname

