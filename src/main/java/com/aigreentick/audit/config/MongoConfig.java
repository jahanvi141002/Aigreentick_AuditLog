package com.aigreentick.audit.config;

import org.springframework.context.annotation.Configuration;

/**
 * MongoDB configuration for database-level auditing
 * 
 * The MongoAuditEventListener is automatically registered via @Component annotation.
 * The MongoAuditWebFilter is automatically registered via @Component annotation.
 * 
 * No additional configuration needed - Spring Boot will auto-configure MongoDB
 * and register the event listeners automatically.
 */
@Configuration
public class MongoConfig {
    // Configuration is handled via @Component annotations on listeners
    // Spring Data MongoDB will automatically detect and register:
    // - MongoAuditEventListener (for database operations)
    // - MongoAuditWebFilter (for HTTP request context)
}

