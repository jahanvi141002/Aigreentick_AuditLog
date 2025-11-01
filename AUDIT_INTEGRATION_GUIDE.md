# Audit Log Integration Guide

This guide shows you how to integrate the audit log functionality into any API in your Spring Boot application.

## Quick Start

### 1. Inject the AuditLogService

```java
@RestController
@RequestMapping("/api/your-resource")
public class YourController {
    
    private final AuditLogService auditLogService;
    
    @Autowired
    public YourController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}
```

## Integration Examples

### Example 1: Logging CREATE Operation

```java
@PostMapping
public ResponseEntity<YourEntity> createEntity(
        @RequestBody YourEntity entity,
        HttpServletRequest request) {
    
    // Save your entity
    YourEntity saved = yourService.save(entity);
    
    // Convert to JSON for audit log
    ObjectMapper mapper = new ObjectMapper();
    String newValueJson = mapper.writeValueAsString(saved);
    
    // Get user info (in real app, get from Spring Security)
    String username = getCurrentUser(request);
    String ipAddress = getClientIpAddress(request);
    
    // Log the CREATE action
    auditLogService.logCreate(
        username,           // Who created it
        "YourEntity",       // Entity name
        saved.getId(),      // Entity ID
        newValueJson,       // New value (JSON)
        ipAddress          // IP address
    );
    
    return ResponseEntity.ok(saved);
}
```

### Example 2: Logging UPDATE Operation

```java
@PutMapping("/{id}")
public ResponseEntity<YourEntity> updateEntity(
        @PathVariable String id,
        @RequestBody YourEntity updatedEntity,
        HttpServletRequest request) {
    
    // Get existing entity
    YourEntity existing = yourService.findById(id);
    
    // Store old value
    ObjectMapper mapper = new ObjectMapper();
    String oldValueJson = mapper.writeValueAsString(existing);
    
    // Update entity
    YourEntity updated = yourService.update(id, updatedEntity);
    String newValueJson = mapper.writeValueAsString(updated);
    
    // Get user info
    String username = getCurrentUser(request);
    String ipAddress = getClientIpAddress(request);
    
    // Log the UPDATE action
    auditLogService.logUpdate(
        username,           // Who updated it
        "YourEntity",       // Entity name
        id,                 // Entity ID
        oldValueJson,       // Old value (JSON)
        newValueJson,       // New value (JSON)
        ipAddress          // IP address
    );
    
    return ResponseEntity.ok(updated);
}
```

### Example 3: Logging DELETE Operation

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteEntity(
        @PathVariable String id,
        HttpServletRequest request) {
    
    // Get entity before deletion
    YourEntity entity = yourService.findById(id);
    
    // Store old value
    ObjectMapper mapper = new ObjectMapper();
    String oldValueJson = mapper.writeValueAsString(entity);
    
    // Delete entity
    yourService.delete(id);
    
    // Get user info
    String username = getCurrentUser(request);
    String ipAddress = getClientIpAddress(request);
    
    // Log the DELETE action
    auditLogService.logDelete(
        username,           // Who deleted it
        "YourEntity",       // Entity name
        id,                 // Entity ID
        oldValueJson,       // Old value (what was deleted)
        ipAddress          // IP address
    );
    
    return ResponseEntity.noContent().build();
}
```

### Example 4: Custom Action Logging

```java
@PostMapping("/{id}/custom-action")
public ResponseEntity<YourEntity> customAction(
        @PathVariable String id,
        HttpServletRequest request) {
    
    // Perform your custom action
    YourEntity entity = yourService.performCustomAction(id);
    
    // Get user info
    String username = getCurrentUser(request);
    String ipAddress = getClientIpAddress(request);
    
    // Log custom action
    auditLogService.log(
        username,           // Who performed the action
        "YourEntity",       // Entity name
        id,                 // Entity ID (optional)
        "CUSTOM_ACTION",    // Action type
        ipAddress          // IP address
    );
    
    return ResponseEntity.ok(entity);
}
```

### Example 5: Simple Logging (without entity ID)

```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest) {
    
    // Perform login
    AuthResponse response = authService.login(request);
    
    // Log login action
    auditLogService.log(
        request.getUsername(),  // Username
        "Authentication",       // Entity name
        "LOGIN",               // Action
        getClientIpAddress(httpRequest)
    );
    
    return ResponseEntity.ok(response);
}
```

## Helper Methods

### Get Current User (with Spring Security)

```java
private String getCurrentUser(HttpServletRequest request) {
    // If using Spring Security:
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
        return authentication.getName();
    }
    return "anonymous";
}
```

### Get Current User (without Spring Security)

```java
private String getCurrentUser(HttpServletRequest request) {
    // Get from custom header
    String username = request.getHeader("X-Username");
    return username != null && !username.isEmpty() ? username : "system";
}
```

### Get Client IP Address

```java
private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
    }
    
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
        return xRealIp;
    }
    
    return request.getRemoteAddr();
}
```

## Available Audit Log Service Methods

### 1. `logCreate()` - For CREATE operations
```java
auditLogService.logCreate(username, entityName, entityId, newValueJson, ipAddress);
```

### 2. `logUpdate()` - For UPDATE operations
```java
auditLogService.logUpdate(username, entityName, entityId, oldValueJson, newValueJson, ipAddress);
```

### 3. `logDelete()` - For DELETE operations
```java
auditLogService.logDelete(username, entityName, entityId, oldValueJson, ipAddress);
```

### 4. `log()` - General purpose logging
```java
// With entity ID only
auditLogService.log(username, entityName, entityId, action);

// With entity ID and IP address
auditLogService.log(username, entityName, entityId, action, ipAddress);

// Without entity ID but with IP address
auditLogService.logWithIp(username, entityName, action, ipAddress);

// Simple logging without entity ID and IP
auditLogService.log(username, entityName, action);
```

## Using with Spring Security

If you're using Spring Security, you can extract user information like this:

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

private String getCurrentUser(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
        return authentication.getName();
    }
    return "anonymous";
}
```

## Querying Audit Logs

You can query audit logs through the REST API:

```bash
# Get all audit logs
GET /api/audit-logs

# Get logs by username
GET /api/audit-logs/username/{username}

# Get logs by entity name
GET /api/audit-logs/entity/{entityName}

# Get logs by action
GET /api/audit-logs/action/{action}

# Get logs by date range
GET /api/audit-logs/date-range?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59

# Get logs by entity
GET /api/audit-logs/entity/{entityName}/{entityId}
```

## Complete Example

See `UserController.java` for a complete working example with:
- CREATE operation logging
- UPDATE operation logging with before/after values
- DELETE operation logging
- GET operation logging
- Error handling

See `InvoiceController.java` for examples with custom actions like APPROVE and REJECT.

