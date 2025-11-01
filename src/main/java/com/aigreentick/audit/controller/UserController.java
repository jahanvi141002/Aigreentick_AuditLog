package com.aigreentick.audit.controller;

import com.aigreentick.audit.model.User;
import com.aigreentick.audit.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // In-memory storage for demo purposes (in real app, use a service/repository)
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final AuditLogService auditLogService;

    @Autowired
    public UserController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
        
        // Initialize with sample data
        User user1 = new User("1", "john.doe", "john@example.com", "John Doe", "ADMIN");
        User user2 = new User("2", "jane.smith", "jane@example.com", "Jane Smith", "USER");
        userStore.put("1", user1);
        userStore.put("2", user2);
    }

    /**
     * Example 1: CREATE Operation - Log when creating a new user
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user, HttpServletRequest request) {
        try {
            // Generate ID if not provided
            if (user.getId() == null || user.getId().isEmpty()) {
                user.setId(UUID.randomUUID().toString());
            }
            
            // Save the user (in real app, use service/repository)
            userStore.put(user.getId(), user);
            
            // Convert new user to JSON for audit log
            String newValueJson = objectMapper.writeValueAsString(user);
            
            // Get client IP address
            String ipAddress = getClientIpAddress(request);
            
            // Get username from request (in real app, get from security context/authentication)
            String auditUsername = getCurrentUser(request);
            
            // Log the CREATE action
            auditLogService.logCreate(
                auditUsername,           // Who created it
                "User",                  // Entity name
                user.getId(),            // Entity ID
                newValueJson,            // New value (JSON)
                ipAddress               // IP address
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
            
        } catch (Exception e) {
            // Log error to audit
            String ipAddress = getClientIpAddress(request);
            auditLogService.logWithIp(
                getCurrentUser(request),
                "User",
                "CREATE_FAILED",
                ipAddress
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Example 2: UPDATE Operation - Log before and after values
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestBody User updatedUser,
            HttpServletRequest request) {
        try {
            // Get existing user
            User existingUser = userStore.get(id);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Store old value for audit log
            String oldValueJson = objectMapper.writeValueAsString(existingUser);
            
            // Update user fields
            if (updatedUser.getUsername() != null) {
                existingUser.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getEmail() != null) {
                existingUser.setEmail(updatedUser.getEmail());
            }
            if (updatedUser.getFullName() != null) {
                existingUser.setFullName(updatedUser.getFullName());
            }
            if (updatedUser.getRole() != null) {
                existingUser.setRole(updatedUser.getRole());
            }
            
            // Save updated user
            userStore.put(id, existingUser);
            
            // Convert updated user to JSON
            String newValueJson = objectMapper.writeValueAsString(existingUser);
            
            // Get client IP address
            String ipAddress = getClientIpAddress(request);
            String auditUsername = getCurrentUser(request);
            
            // Log the UPDATE action with before and after values
            auditLogService.logUpdate(
                auditUsername,           // Who updated it
                "User",                  // Entity name
                id,                      // Entity ID
                oldValueJson,            // Old value (JSON)
                newValueJson,            // New value (JSON)
                ipAddress               // IP address
            );
            
            return ResponseEntity.ok(existingUser);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Example 3: DELETE Operation - Log before deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id, HttpServletRequest request) {
        try {
            // Get user before deletion
            User userToDelete = userStore.get(id);
            if (userToDelete == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Store old value for audit log
            String oldValueJson = objectMapper.writeValueAsString(userToDelete);
            
            // Delete the user
            userStore.remove(id);
            
            // Get client IP address
            String ipAddress = getClientIpAddress(request);
            String auditUsername = getCurrentUser(request);
            
            // Log the DELETE action
            auditLogService.logDelete(
                auditUsername,           // Who deleted it
                "User",                  // Entity name
                id,                      // Entity ID
                oldValueJson,            // Old value (what was deleted)
                ipAddress               // IP address
            );
            
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Example 4: GET Operation - Simple read (might want to log sensitive data access)
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id, HttpServletRequest request) {
        User user = userStore.get(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Optionally log access to sensitive data
        String ipAddress = getClientIpAddress(request);
        auditLogService.log(
            getCurrentUser(request),
            "User",
            id,
            "READ",
            ipAddress
        );
        
        return ResponseEntity.ok(user);
    }

    /**
     * Example 5: GET All - List operation
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(new ArrayList<>(userStore.values()));
    }

    /**
     * Helper method to extract client IP address from request
     */
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

    /**
     * Helper method to get current user (in real app, extract from Spring Security context)
     * For demo purposes, we'll use a header or default to "system"
     */
    private String getCurrentUser(HttpServletRequest request) {
        // In a real application, you would get this from Spring Security:
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // return authentication.getName();
        
        // For demo, check if username is in header, otherwise use "system"
        String username = request.getHeader("X-Username");
        return username != null && !username.isEmpty() ? username : "system";
    }
}

