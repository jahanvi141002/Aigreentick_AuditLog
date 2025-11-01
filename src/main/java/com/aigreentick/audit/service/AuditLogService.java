package com.aigreentick.audit.service;

import com.aigreentick.audit.model.AuditLog;
import com.aigreentick.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Create a new audit log entry
     */
    public AuditLog createAuditLog(AuditLog auditLog) {
        if (auditLog.getTimestamp() == null) {
            auditLog.setTimestamp(LocalDateTime.now());
        }
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log with common parameters
     */
    public AuditLog log(String username, String entityName, String action) {
        AuditLog auditLog = new AuditLog(username, entityName, action);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log with entity ID
     */
    public AuditLog log(String username, String entityName, String entityId, String action) {
        AuditLog auditLog = new AuditLog(username, entityName, action);
        auditLog.setEntityId(entityId);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log with entity ID and IP address
     */
    public AuditLog log(String username, String entityName, String entityId, String action, String ipAddress) {
        AuditLog auditLog = new AuditLog(username, entityName, action);
        auditLog.setEntityId(entityId);
        auditLog.setIpAddress(ipAddress);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log with action and IP address (without entity ID)
     * Note: Method name differs to avoid signature conflict
     */
    public AuditLog logWithIp(String username, String entityName, String action, String ipAddress) {
        AuditLog auditLog = new AuditLog(username, entityName, action);
        auditLog.setIpAddress(ipAddress);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log with old and new values (for UPDATE operations)
     */
    public AuditLog logUpdate(String username, String entityName, String entityId, 
                              String oldValue, String newValue, String ipAddress) {
        AuditLog auditLog = new AuditLog(username, entityName, "UPDATE");
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log for CREATE operation
     */
    public AuditLog logCreate(String username, String entityName, String entityId, 
                             String newValue, String ipAddress) {
        AuditLog auditLog = new AuditLog(username, entityName, "CREATE");
        auditLog.setEntityId(entityId);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log for DELETE operation
     */
    public AuditLog logDelete(String username, String entityName, String entityId, 
                             String oldValue, String ipAddress) {
        AuditLog auditLog = new AuditLog(username, entityName, "DELETE");
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(oldValue);
        auditLog.setIpAddress(ipAddress);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Get all audit logs
     */
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    /**
     * Get audit log by ID
     */
    public Optional<AuditLog> getAuditLogById(String id) {
        return auditLogRepository.findById(id);
    }

    /**
     * Get audit logs by username
     */
    public List<AuditLog> getAuditLogsByUsername(String username) {
        return auditLogRepository.findByUsername(username);
    }

    /**
     * Get audit logs by entity name
     */
    public List<AuditLog> getAuditLogsByEntityName(String entityName) {
        return auditLogRepository.findByEntityName(entityName);
    }

    /**
     * Get audit logs by action
     */
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    /**
     * Get audit logs by entity name and entity ID
     */
    public List<AuditLog> getAuditLogsByEntity(String entityName, String entityId) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId);
    }

    /**
     * Get audit logs by timestamp range
     */
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    /**
     * Get audit logs by username and date range
     */
    public List<AuditLog> getAuditLogsByUsernameAndDateRange(String username, 
                                                             LocalDateTime start, 
                                                             LocalDateTime end) {
        return auditLogRepository.findByUsernameAndTimestampBetween(username, start, end);
    }

    /**
     * Get audit logs by entity name and date range
     */
    public List<AuditLog> getAuditLogsByEntityNameAndDateRange(String entityName, 
                                                               LocalDateTime start, 
                                                               LocalDateTime end) {
        return auditLogRepository.findByEntityNameAndTimestampBetween(entityName, start, end);
    }
}

