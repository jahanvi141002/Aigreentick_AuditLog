package com.aigreentick.audit.service;

import com.aigreentick.audit.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AuditLogKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaProducer.class);

    @Value("${spring.kafka.topic.audit-logs}")
    private String auditLogsTopic;

    @Autowired
    private KafkaTemplate<String, AuditLog> kafkaTemplate;

    public void sendAuditLog(AuditLog auditLog) {
        try {
            String key = auditLog.getEntityName() + "-" + auditLog.getEntityId();
            
            CompletableFuture<SendResult<String, AuditLog>> future = 
                kafkaTemplate.send(auditLogsTopic, key, auditLog);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Audit log successfully sent to Kafka topic: {}, partition: {}, offset: {}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send audit log to Kafka: {} on {}", 
                        auditLog.getAction(), auditLog.getEntityName(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error sending audit log to Kafka", e);
        }
    }
}

