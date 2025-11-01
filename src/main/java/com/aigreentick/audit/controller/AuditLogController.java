package com.aigreentick.audit.controller;

import com.aigreentick.audit.model.AuditLog;
import com.aigreentick.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Autowired
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new audit log entry
     */
    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(@RequestBody AuditLog auditLog, 
                                                   HttpServletRequest request) {
        // Extract IP address from request
        String ipAddress = getClientIpAddress(request);
        if (auditLog.getIpAddress() == null) {
            auditLog.setIpAddress(ipAddress);
        }
        
        AuditLog savedLog = auditLogService.createAuditLog(auditLog);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLog);
    }

    /**
     * Get all audit logs
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> auditLogs = auditLogService.getAllAuditLogs();
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable String id) {
        Optional<AuditLog> auditLog = auditLogService.getAuditLogById(id);
        return auditLog.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get audit logs by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUsername(@PathVariable String username) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByUsername(username);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by entity name
     */
    @GetMapping("/entity/{entityName}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntityName(@PathVariable String entityName) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByEntityName(entityName);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by action
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(@PathVariable String action) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByAction(action);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by entity name and entity ID
     */
    @GetMapping("/entity/{entityName}/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntity(
            @PathVariable String entityName,
            @PathVariable String entityId) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByEntity(entityName, entityId);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByDateRange(start, end);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by username and date range
     */
    @GetMapping("/username/{username}/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUsernameAndDateRange(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByUsernameAndDateRange(username, start, end);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by entity name and date range
     */
    @GetMapping("/entity/{entityName}/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntityNameAndDateRange(
            @PathVariable String entityName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AuditLog> auditLogs = auditLogService.getAuditLogsByEntityNameAndDateRange(entityName, start, end);
        return ResponseEntity.ok(auditLogs);
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
}

