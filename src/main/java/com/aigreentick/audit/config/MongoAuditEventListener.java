package com.aigreentick.audit.config;

import com.aigreentick.audit.model.AuditLog;
import com.aigreentick.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Database-level audit listener that automatically captures all MongoDB operations
 * This intercepts save, delete, and other operations at the database level
 */
@Component
public class MongoAuditEventListener extends AbstractMongoEventListener<Object> {

    private static final Logger logger = LoggerFactory.getLogger(MongoAuditEventListener.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private MongoTemplate mongoTemplate;
    
    // Collections to exclude from auditing (e.g., audit_logs itself)
    private static final String AUDIT_LOGS_COLLECTION = "audit_logs";
    
    // Thread-local storage for old values (to capture before UPDATE)
    private static final ThreadLocal<org.bson.Document> oldValueStorage = new ThreadLocal<>();

    @Autowired
    public MongoAuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    @Autowired(required = false)
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * Before convert - Capture old value for UPDATE operations
     */
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        try {
            if (AUDIT_LOGS_COLLECTION.equals(event.getCollectionName())) {
                return;
            }

            Object entity = event.getSource();
            String entityId = extractEntityId(entity);
            
            // If entity has an ID, it's an UPDATE - fetch old value
            if (entityId != null && mongoTemplate != null) {
                try {
                    org.bson.Document oldDocument = mongoTemplate.findById(entityId, org.bson.Document.class, event.getCollectionName());
                    if (oldDocument != null) {
                        oldValueStorage.set(oldDocument);
                    }
                } catch (Exception e) {
                    logger.debug("Could not fetch old document for audit", e);
                }
            }
        } catch (Exception e) {
            logger.debug("Error in before convert event", e);
        }
    }

    /**
     * Captures CREATE and UPDATE operations (AfterSaveEvent is triggered for both)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<Object> event) {
        try {
            // Skip auditing audit logs themselves to avoid recursion
            if (AUDIT_LOGS_COLLECTION.equals(event.getCollectionName())) {
                return;
            }

            Object entity = event.getSource();
            String entityId = extractEntityId(entity);
            String entityName = extractEntityName(entity);
            
            // Get user context from thread-local
            String username = MongoAuditContext.getUsername();
            String ipAddress = MongoAuditContext.getIpAddress();
            
            // If no username set, use "system"
            if (username == null || username.isEmpty()) {
                username = "system";
            }

            // Determine action: CREATE if ID was just generated, UPDATE otherwise
            String action = isNewEntity(entity) ? "CREATE" : "UPDATE";

            // Convert entity to JSON
            String newValueJson = null;
            try {
                newValueJson = objectMapper.writeValueAsString(entity);
            } catch (Exception e) {
                logger.warn("Failed to serialize entity to JSON for audit log", e);
                newValueJson = entity.toString();
            }

            // Get old value for UPDATE operations
            String oldValueJson = null;
            if ("UPDATE".equals(action)) {
                org.bson.Document oldDocument = oldValueStorage.get();
                if (oldDocument != null) {
                    try {
                        oldValueJson = objectMapper.writeValueAsString(oldDocument);
                    } catch (Exception e) {
                        oldValueJson = oldDocument.toJson();
                    }
                    oldValueStorage.remove(); // Clean up
                }
            }

            // Create audit log
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setOldValue(oldValueJson);
            auditLog.setNewValue(newValueJson);
            auditLog.setIpAddress(ipAddress);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription(String.format("Database %s operation on %s", action, entityName));

            // Save audit log (this will NOT trigger another audit event because we check collection name)
            auditLogRepository.save(auditLog);

            logger.debug("Audit log created for {} operation on {}", action, entityName);

        } catch (Exception e) {
            logger.error("Error creating audit log for save operation", e);
            // Don't throw exception to avoid breaking the main operation
        }
    }

    /**
     * Captures DELETE operations
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<Object> event) {
        try {
            // Skip auditing audit logs themselves
            if (AUDIT_LOGS_COLLECTION.equals(event.getCollectionName())) {
                return;
            }

            String entityId = extractIdFromDocument(event.getDocument());
            String entityName = extractEntityNameFromCollection(event.getCollectionName());

            // Get user context
            String username = MongoAuditContext.getUsername();
            String ipAddress = MongoAuditContext.getIpAddress();

            if (username == null || username.isEmpty()) {
                username = "system";
            }

            // Get the deleted document as JSON
            String oldValueJson = null;
            try {
                oldValueJson = objectMapper.writeValueAsString(event.getDocument());
            } catch (Exception e) {
                logger.warn("Failed to serialize deleted document to JSON", e);
                oldValueJson = event.getDocument().toJson();
            }

            // Create audit log
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setAction("DELETE");
            auditLog.setOldValue(oldValueJson);
            auditLog.setIpAddress(ipAddress);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription(String.format("Database DELETE operation on %s", entityName));

            auditLogRepository.save(auditLog);

            logger.debug("Audit log created for DELETE operation on {}", entityName);

        } catch (Exception e) {
            logger.error("Error creating audit log for delete operation", e);
        }
    }

    /**
     * Extract entity ID from the entity object
     */
    private String extractEntityId(Object entity) {
        try {
            // Try reflection to get id field
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            return idValue != null ? idValue.toString() : null;
        } catch (Exception e) {
            // If entity has @Id annotation or uses getId() method
            try {
                java.lang.reflect.Method getIdMethod = entity.getClass().getMethod("getId");
                Object idValue = getIdMethod.invoke(entity);
                return idValue != null ? idValue.toString() : null;
            } catch (Exception ex) {
                logger.warn("Could not extract ID from entity: {}", entity.getClass().getName());
                return null;
            }
        }
    }

    /**
     * Extract entity name from the entity object (class name without package)
     */
    private String extractEntityName(Object entity) {
        String className = entity.getClass().getSimpleName();
        // Remove common suffixes
        if (className.endsWith("Entity")) {
            className = className.substring(0, className.length() - 6);
        }
        return className;
    }

    /**
     * Extract entity name from collection name
     */
    private String extractEntityNameFromCollection(String collectionName) {
        // Convert collection name to entity name (e.g., "users" -> "User")
        if (collectionName == null || collectionName.isEmpty()) {
            return "Unknown";
        }
        // Capitalize first letter and make singular if possible
        String name = collectionName.substring(0, 1).toUpperCase() + collectionName.substring(1);
        // Remove trailing 's' if plural
        if (name.endsWith("s") && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * Extract ID from MongoDB document
     */
    private String extractIdFromDocument(org.bson.Document document) {
        if (document != null && document.containsKey("_id")) {
            Object id = document.get("_id");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    /**
     * Check if entity is new (just created)
     * This is a simple heuristic - you might want to enhance this
     */
    private boolean isNewEntity(Object entity) {
        try {
            java.lang.reflect.Method getIdMethod = entity.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            // If ID is null or empty, it's likely a new entity
            return id == null || id.toString().isEmpty();
        } catch (Exception e) {
            // Default to UPDATE if we can't determine
            return false;
        }
    }
}

