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
     * Create a new audit log entry (for manual logging if needed)
     */
    public AuditLog createAuditLog(AuditLog auditLog) {
        if (auditLog.getTimestamp() == null) {
            auditLog.setTimestamp(LocalDateTime.now());
        }
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

