package com.aigreentick.audit.repository;

import com.aigreentick.audit.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    // Find by username
    List<AuditLog> findByUsername(String username);

    // Find by entity name
    List<AuditLog> findByEntityName(String entityName);

    // Find by action
    List<AuditLog> findByAction(String action);

    // Find by entity name and entity ID
    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    // Find by timestamp range
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Find by username and timestamp range
    List<AuditLog> findByUsernameAndTimestampBetween(String username, LocalDateTime start, LocalDateTime end);

    // Find by entity name and timestamp range
    List<AuditLog> findByEntityNameAndTimestampBetween(String entityName, LocalDateTime start, LocalDateTime end);
}

