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
     * Get audit logs by user ID
     */
    public List<AuditLog> getAuditLogsByUserId(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    /**
     * Get audit logs by organization ID
     */
    public List<AuditLog> getAuditLogsByOrganizationId(String organizationId) {
        return auditLogRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get audit logs by URL domain
     */
    public List<AuditLog> getAuditLogsByUrlDomain(String urlDomain) {
        return auditLogRepository.findByUrlDomain(urlDomain);
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

    /**
     * Get audit logs by user ID and date range
     */
    public List<AuditLog> getAuditLogsByUserIdAndDateRange(String userId, 
                                                           LocalDateTime start, 
                                                           LocalDateTime end) {
        return auditLogRepository.findByUserIdAndTimestampBetween(userId, start, end);
    }

    /**
     * Get audit logs by organization ID and date range
     */
    public List<AuditLog> getAuditLogsByOrganizationIdAndDateRange(String organizationId, 
                                                                  LocalDateTime start, 
                                                                  LocalDateTime end) {
        return auditLogRepository.findByOrganizationIdAndTimestampBetween(organizationId, start, end);
    }

    /**
     * Get audit logs by URL domain and date range
     */
    public List<AuditLog> getAuditLogsByUrlDomainAndDateRange(String urlDomain, 
                                                              LocalDateTime start, 
                                                              LocalDateTime end) {
        return auditLogRepository.findByUrlDomainAndTimestampBetween(urlDomain, start, end);
    }
}

