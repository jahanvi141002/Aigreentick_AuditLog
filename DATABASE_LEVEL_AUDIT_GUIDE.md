# Database-Level Audit Logging Guide

This guide explains the **database-level audit logging** system that automatically captures all MongoDB operations without requiring manual code in your controllers or services.

## How It Works

The system uses **Spring Data MongoDB Event Listeners** to intercept all database operations (save, delete) at the database level. This means:

- ✅ **No manual audit code needed** - Operations are automatically audited
- ✅ **Captures all database changes** - Even direct repository calls are audited
- ✅ **Before/After values** - Automatically captures old and new values
- ✅ **Thread-safe** - Uses thread-local context for user information

## Architecture

### Components

1. **MongoAuditEventListener** - Intercepts MongoDB save/delete operations
2. **MongoAuditContext** - Thread-local storage for user context (username, IP)
3. **MongoAuditWebFilter** - Captures user info from HTTP requests

### Flow

```
HTTP Request → MongoAuditWebFilter (captures username/IP)
    ↓
Controller/Service → Repository.save() or Repository.delete()
    ↓
MongoAuditEventListener (intercepts at DB level)
    ↓
AuditLog created automatically
```

## Usage

### Example 1: Simple CRUD Operations (Auto-Audited)

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Just save - audit happens automatically!
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
        // ✅ CREATE audit log is automatically created
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, 
                                          @RequestBody User user) {
        User existing = userRepository.findById(id).orElseThrow();
        // Update fields...
        User saved = userRepository.save(existing);
        return ResponseEntity.ok(saved);
        // ✅ UPDATE audit log with old/new values automatically created
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
        // ✅ DELETE audit log with deleted document automatically created
    }
}
```

### Example 2: Setting User Context

The system automatically captures user info from HTTP headers:

```bash
# Include username in header
POST http://localhost:8081/api/users
X-Username: john.doe
Content-Type: application/json

{
  "username": "test.user",
  "email": "test@example.com"
}
```

If no `X-Username` header is provided, it defaults to:
- `"anonymous"` for HTTP requests
- `"system"` for non-HTTP operations

### Example 3: Programmatic User Context (for Background Jobs)

```java
@Service
public class ScheduledJobService {
    
    @Autowired
    private UserRepository userRepository;
    
    public void runScheduledTask() {
        // Set user context for background operations
        MongoAuditContext.setUsername("system-job");
        MongoAuditContext.setIpAddress("127.0.0.1");
        
        try {
            // Perform operations - they will be audited
            userRepository.save(newUser);
            userRepository.delete(oldUser);
        } finally {
            // Always clear context
            MongoAuditContext.clear();
        }
    }
}
```

## What Gets Audited

### Automatically Captured:

- ✅ **Entity Type** - Extracted from class name (e.g., "User", "Invoice")
- ✅ **Entity ID** - Extracted from `@Id` field or `getId()` method
- ✅ **Action** - CREATE, UPDATE, or DELETE
- ✅ **Timestamp** - When the operation occurred
- ✅ **Username** - From HTTP header `X-Username` or context
- ✅ **IP Address** - From HTTP request headers
- ✅ **New Value** - Complete entity JSON (for CREATE/UPDATE)
- ✅ **Old Value** - Complete entity JSON (for DELETE/UPDATE)

### Collections Excluded:

- `audit_logs` - Prevents infinite recursion

## Querying Database Audit Logs

All database operations are stored in the `audit_logs` collection. Query them using:

```bash
# Get all audit logs
GET http://localhost:8081/api/audit-logs

# Get logs for specific user
GET http://localhost:8081/api/audit-logs/username/john.doe

# Get logs for specific entity type
GET http://localhost:8081/api/audit-logs/entity/User

# Get logs by action
GET http://localhost:8081/api/audit-logs/action/CREATE

# Get logs for specific entity
GET http://localhost:8081/api/audit-logs/entity/User/user123
```

## Comparison: Manual vs Database-Level Auditing

### Manual Auditing (Previous Approach)
```java
@PostMapping
public ResponseEntity<User> createUser(@RequestBody User user, 
                                      HttpServletRequest request) {
    User saved = userRepository.save(user);
    
    // Manual audit code required
    String newValueJson = objectMapper.writeValueAsString(saved);
    String username = getCurrentUser(request);
    String ipAddress = getClientIpAddress(request);
    auditLogService.logCreate(username, "User", saved.getId(), 
                              newValueJson, ipAddress);
    
    return ResponseEntity.ok(saved);
}
```

### Database-Level Auditing (New Approach)
```java
@PostMapping
public ResponseEntity<User> createUser(@RequestBody User user) {
    // Just save - audit happens automatically!
    User saved = userRepository.save(user);
    return ResponseEntity.ok(saved);
}
```

## Configuration

### Enable/Disable for Specific Collections

To exclude a collection from auditing, modify `MongoAuditEventListener`:

```java
private static final Set<String> EXCLUDED_COLLECTIONS = Set.of(
    "audit_logs",
    "system_logs",
    "cache_data"
);

@Override
public void onAfterSave(AfterSaveEvent<Object> event) {
    if (EXCLUDED_COLLECTIONS.contains(event.getCollectionName())) {
        return;
    }
    // ... audit logic
}
```

### Customize Entity Name Extraction

Modify `extractEntityName()` method in `MongoAuditEventListener` to customize how entity names are derived from class names.

## Testing

### Test Database-Level Auditing

```bash
# 1. Create a user (will auto-audit)
POST http://localhost:8081/api/demo/users
X-Username: admin
Content-Type: application/json

{
  "username": "test.user",
  "email": "test@example.com",
  "fullName": "Test User",
  "role": "USER"
}

# 2. Check audit logs
GET http://localhost:8081/api/audit-logs

# You should see a CREATE audit log automatically created!
```

## Benefits

1. **Zero Boilerplate** - No need to add audit code in every controller
2. **Comprehensive** - Captures ALL database operations, even direct repository calls
3. **Consistent** - Same audit format for all entities
4. **Maintainable** - Audit logic in one place
5. **Non-Intrusive** - Doesn't affect your business logic

## Limitations

1. **Only MongoDB** - This implementation is MongoDB-specific
2. **No Before-Update Values** - For UPDATE operations, we can't capture the "before" value easily (MongoDB doesn't provide it in save events)
3. **Performance** - Adds slight overhead for every database operation
4. **User Context** - Requires HTTP requests or manual context setting for background jobs

## Troubleshooting

### Audit logs not being created

1. Check MongoDB connection is working
2. Verify `MongoAuditEventListener` is registered (check startup logs)
3. Ensure collection is not in excluded list
4. Check logs for exceptions in audit listener

### Wrong username in audit logs

- Ensure `X-Username` header is set in HTTP requests
- For background jobs, manually set context using `MongoAuditContext.setUsername()`

### Recursive audit logs

- The system automatically excludes `audit_logs` collection
- If you create audit logs manually, ensure you exclude the audit collection

## Advanced: MongoDB Change Streams

For even more comprehensive auditing (including updates made directly to MongoDB), you can implement MongoDB Change Streams:

```java
@Component
public class MongoChangeStreamListener {
    
    @PostConstruct
    public void listenToChanges() {
        MongoCollection<Document> collection = mongoTemplate.getCollection("users");
        collection.watch().forEach(change -> {
            // Process change events
            // This captures ALL changes, even direct MongoDB operations
        });
    }
}
```

This is more complex but provides complete database-level auditing.

