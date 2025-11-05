package com.aigreentick.audit.service;

import com.aigreentick.audit.model.AuditLog;
import com.aigreentick.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AuditLogKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaConsumer.class);
    
    @Value("${audit.consumer.batch-size:10}")
    private int batchSize;

    @Autowired
    private AuditLogRepository auditLogRepository;
    
    // In-memory buffer to accumulate records
    private final List<AuditLog> buffer = new ArrayList<>();
    private final Lock bufferLock = new ReentrantLock();
    
    @PostConstruct
    public void init() {
        logger.info("=== AuditLogKafkaConsumer initialized with BATCH SAVING ({} records) ===", batchSize);
        logger.info("=== Ready to consume messages from topic: audit-logs ===");
        logger.info("=== Will save exactly {} records in each batch ===", batchSize);
        logger.info("=== Buffer size: {} records ===", batchSize);
    }
    
    @PreDestroy
    public void cleanup() {
        // Save any remaining records in buffer when application shuts down
        bufferLock.lock();
        try {
            if (!buffer.isEmpty()) {
                logger.info("Application shutting down, saving {} remaining audit logs...", buffer.size());
                auditLogRepository.saveAll(buffer);
                buffer.clear();
                logger.info("Saved remaining audit logs successfully");
            }
        } finally {
            bufferLock.unlock();
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.audit-logs}", 
                   containerFactory = "kafkaListenerContainerFactory",
                   groupId = "${spring.kafka.consumer.group-id}",
                   autoStartup = "true",
                   id = "auditLogKafkaListener")
    public void consumeBatchAuditLogs(
            @Payload List<AuditLog> auditLogs,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("=== CONSUMER CALLED ===");
            logger.info("Received {} audit logs from topic {}", auditLogs.size(), topic);

            if (auditLogs == null || auditLogs.isEmpty()) {
                logger.warn("Received empty or null batch of audit logs");
                acknowledgment.acknowledge();
                return;
            }

            bufferLock.lock();
            try {
                // Add received records to buffer
                buffer.addAll(auditLogs);
                logger.info("Added {} records to buffer. Total in buffer: {}", 
                    auditLogs.size(), buffer.size());

                // Process batches of exactly batchSize records
                while (buffer.size() >= batchSize) {
                    // Extract exactly batchSize records from buffer
                    List<AuditLog> batchToSave = new ArrayList<>(buffer.subList(0, batchSize));
                    // Remove first batchSize records from buffer
                    buffer.subList(0, batchSize).clear();
                    
                    logger.info("=== SAVING BATCH OF EXACTLY {} RECORDS ===", batchSize);
                    logger.info("Buffer now has {} records remaining", buffer.size());
                    
                    // Save exactly batchSize records to database
                    auditLogRepository.saveAll(batchToSave);
                    logger.info("✅ Successfully saved exactly {} audit logs to database", batchSize);
                }
                
                // Log if buffer has records but less than batchSize
                if (buffer.size() > 0 && buffer.size() < batchSize) {
                    logger.info("Buffer has {} records (waiting for {} more to reach batch size of {})", 
                        buffer.size(), batchSize - buffer.size(), batchSize);
                }
                
            } finally {
                bufferLock.unlock();
            }

            // Acknowledge all received records
            acknowledgment.acknowledge();
            logger.info("Acknowledged {} audit logs", auditLogs.size());

        } catch (Exception e) {
            logger.error("Error processing batch of audit logs", e);
            logger.error("Exception details: {}", e.getMessage(), e);
            // Don't acknowledge on error - let Kafka retry
        }
    }
}

