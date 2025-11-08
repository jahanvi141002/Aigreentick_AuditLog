package com.aigreentick.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;
    
    // Who did the action
    @Field(name = "username")
    private String username;
    
    @Field(name = "user_id")
    private String userId;
    
    @Field(name = "organization_id")
    private String organizationId;
    
    @Field(name = "url_domain")
    private String urlDomain;

    // Which module or entity type (e.g., "User", "Invoice")
    @Field(name = "entity_name")
    private String entityName;

    // ID of the affected entity (if applicable)
    @Field(name = "entity_id")
    private String entityId;

    // Type of action: CREATE / UPDATE / DELETE / LOGIN / LOGOUT etc.
    @Field(name = "action")
    private String action;

    // Optional JSON or text field for before-after values
    @Field(name = "old_value")
    private String oldValue;

    @Field(name = "new_value")
    private String newValue;

    // Description (optional for extra info)
    @Field(name = "description")
    private String description;

    // When it happened
    @Field(name = "timestamp")
    private LocalDateTime timestamp;

    // IP address or user agent (optional)
    @Field(name = "ip_address")
    private String ipAddress;

    // Default constructor
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with required fields
    public AuditLog(String username, String entityName, String action) {
        this.username = username;
        this.entityName = entityName;
        this.action = action;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getUrlDomain() {
        return urlDomain;
    }

    public void setUrlDomain(String urlDomain) {
        this.urlDomain = urlDomain;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", userId='" + userId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", urlDomain='" + urlDomain + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityId='" + entityId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}

