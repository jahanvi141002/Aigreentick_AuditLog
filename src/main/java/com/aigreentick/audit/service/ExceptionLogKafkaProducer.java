package com.aigreentick.audit.service;

import com.aigreentick.audit.model.ExceptionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ExceptionLogKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionLogKafkaProducer.class);

    @Value("${spring.kafka.topic.exception-logs}")
    private String exceptionLogsTopic;

    @Autowired
    private KafkaTemplate<String, ExceptionLog> exceptionLogKafkaTemplate;

    public void sendExceptionLog(ExceptionLog exceptionLog) {
        try {
            // Use exception type and class name as key for partitioning
            String key = exceptionLog.getExceptionType() + "-" + 
                        (exceptionLog.getClassName() != null ? exceptionLog.getClassName() : "unknown");
            
            CompletableFuture<SendResult<String, ExceptionLog>> future = 
                exceptionLogKafkaTemplate.send(exceptionLogsTopic, key, exceptionLog);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Exception log successfully sent to Kafka topic: {}, partition: {}, offset: {}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send exception log to Kafka: {} in {}", 
                        exceptionLog.getExceptionType(), exceptionLog.getClassName(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error sending exception log to Kafka", e);
        }
    }
}

