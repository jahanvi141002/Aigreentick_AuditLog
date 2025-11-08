package com.aigreentick.audit.config;

import com.aigreentick.audit.model.AuditLog;
import com.aigreentick.audit.service.AuditLogKafkaProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

/**
 * Database-level audit listener that automatically captures all MongoDB operations
 * This intercepts save, delete, and other operations at the database level
 */
@Component
public class MongoAuditEventListener extends AbstractMongoEventListener<Object> {

    private static final Logger logger = LoggerFactory.getLogger(MongoAuditEventListener.class);
    
    @Value("${audit.collection.name:audit_logs}")
    private String auditLogsCollection;
    
    @Value("${audit.default.username:system}")
    private String defaultUsername;
    
    private final AuditLogKafkaProducer auditLogKafkaProducer;
    private final ObjectMapper objectMapper;
    private MongoTemplate mongoTemplate;
    
    private static final ThreadLocal<org.bson.Document> oldValueStorage = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> isNewEntityFlag = new ThreadLocal<>();

    @Autowired
    public MongoAuditEventListener(AuditLogKafkaProducer auditLogKafkaProducer, MongoTemplate mongoTemplate) {
        this.auditLogKafkaProducer = auditLogKafkaProducer;
        this.objectMapper = new ObjectMapper();
        this.mongoTemplate = mongoTemplate;
        logger.info("=== MongoAuditEventListener constructor called with MongoTemplate ===");
    }
    
    @PostConstruct
    public void init() {
        logger.info("=== MongoAuditEventListener initialized and registered ===");
        logger.info("=== Will listen for MongoDB operations on all collections except {} ===", auditLogsCollection);
        logger.info("=== Default username: {} ===", defaultUsername);
    }
    
    /**
     * Before convert - Capture old value for UPDATE operations
     */
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        try {
            if (auditLogsCollection.equals(event.getCollectionName())) {
                return;
            }

            Object entity = event.getSource();
            String entityId = extractEntityId(entity);
            
            // Check if entity is new (ID is null before MongoDB generates it)
            boolean isNew = (entityId == null || entityId.isEmpty());
            isNewEntityFlag.set(isNew);
            
            // If it's an update, fetch the old document
            if (!isNew && mongoTemplate != null) {
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
            logger.info("=== onAfterSave CALLED for collection: {} ===", event.getCollectionName());
            
            if (auditLogsCollection.equals(event.getCollectionName())) {
                logger.debug("Skipping audit log for {} collection itself", auditLogsCollection);
                return;
            }

            logger.info("onAfterSave triggered for collection: {}", event.getCollectionName());

            Object entity = event.getSource();
            String entityId = extractEntityId(entity);
            String entityName = extractEntityName(entity);
            
            String username = MongoAuditContext.getUsername();
            String userId = MongoAuditContext.getUserId();
            String organizationId = MongoAuditContext.getOrganizationId();
            String urlDomain = MongoAuditContext.getUrlDomain();
            String ipAddress = MongoAuditContext.getIpAddress();
            
            if (username == null || username.isEmpty()) {
                username = defaultUsername;
            }

            // Use the flag set in onBeforeConvert
            Boolean isNew = isNewEntityFlag.get();
            if (isNew == null) {
                // Fallback to checking entity ID (shouldn't happen)
                isNew = isNewEntity(entity);
            }
            String action = isNew ? "CREATE" : "UPDATE";
            
            // Clear the flag
            isNewEntityFlag.remove();
            String newValueJson = null;
            try {
                newValueJson = objectMapper.writeValueAsString(entity);
            } catch (Exception e) {
                logger.warn("Failed to serialize entity to JSON for audit log", e);
                newValueJson = entity.toString();
            }

            String oldValueJson = null;
            if ("UPDATE".equals(action)) {
                org.bson.Document oldDocument = oldValueStorage.get();
                if (oldDocument != null) {
                    try {
                        oldValueJson = objectMapper.writeValueAsString(oldDocument);
                    } catch (Exception e) {
                        oldValueJson = oldDocument.toJson();
                    }
                    oldValueStorage.remove();
                }
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setUserId(userId);
            auditLog.setOrganizationId(organizationId);
            auditLog.setUrlDomain(urlDomain);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setOldValue(oldValueJson);
            auditLog.setNewValue(newValueJson);
            auditLog.setIpAddress(ipAddress);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription(String.format("Database %s operation on %s", action, entityName));

            logger.info("Sending audit log to Kafka: action={}, entity={}, entityId={}, username={}", 
                action, entityName, entityId, username);
            
            auditLogKafkaProducer.sendAuditLog(auditLog);

            logger.info("Audit log sent to Kafka for {} operation on {}", action, entityName);

        } catch (Exception e) {
            logger.error("Error creating audit log for save operation", e);
        }
    }

    /**
     * Captures DELETE operations
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<Object> event) {
        try {
            if (auditLogsCollection.equals(event.getCollectionName())) {
                return;
            }

            String entityId = extractIdFromDocument(event.getDocument());
            String entityName = extractEntityNameFromCollection(event.getCollectionName());

            String username = MongoAuditContext.getUsername();
            String userId = MongoAuditContext.getUserId();
            String organizationId = MongoAuditContext.getOrganizationId();
            String urlDomain = MongoAuditContext.getUrlDomain();
            String ipAddress = MongoAuditContext.getIpAddress();

            if (username == null || username.isEmpty()) {
                username = "system";
            }

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
            auditLog.setUserId(userId);
            auditLog.setOrganizationId(organizationId);
            auditLog.setUrlDomain(urlDomain);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setAction("DELETE");
            auditLog.setOldValue(oldValueJson);
            auditLog.setIpAddress(ipAddress);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDescription(String.format("Database DELETE operation on %s", entityName));

            auditLogKafkaProducer.sendAuditLog(auditLog);

            logger.debug("Audit log sent to Kafka for DELETE operation on {}", entityName);

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

