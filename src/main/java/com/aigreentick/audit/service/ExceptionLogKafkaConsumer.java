package com.aigreentick.audit.service;

import com.aigreentick.audit.model.ExceptionLog;
import com.aigreentick.audit.repository.ExceptionLogRepository;
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
public class ExceptionLogKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionLogKafkaConsumer.class);
    
    @Value("${exception.consumer.batch-size:10}")
    private int batchSize;

    @Autowired
    private ExceptionLogRepository exceptionLogRepository;
    
    // In-memory buffer to accumulate records
    private final List<ExceptionLog> buffer = new ArrayList<>();
    private final Lock bufferLock = new ReentrantLock();
    
    @PostConstruct
    public void init() {
        logger.info("=== ExceptionLogKafkaConsumer initialized with BATCH SAVING ({} records) ===", batchSize);
        logger.info("=== Ready to consume messages from topic: exception-logs ===");
        logger.info("=== Will save exactly {} records in each batch ===", batchSize);
        logger.info("=== Buffer size: {} records ===", batchSize);
    }
    
    @PreDestroy
    public void cleanup() {
        // Save any remaining records in buffer when application shuts down
        bufferLock.lock();
        try {
            if (!buffer.isEmpty()) {
                logger.info("Application shutting down, saving {} remaining exception logs...", buffer.size());
                exceptionLogRepository.saveAll(buffer);
                buffer.clear();
                logger.info("Saved remaining exception logs successfully");
            }
        } finally {
            bufferLock.unlock();
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.exception-logs}", 
                   containerFactory = "exceptionLogKafkaListenerContainerFactory",
                   groupId = "${spring.kafka.consumer.group-id}",
                   autoStartup = "true",
                   id = "exceptionLogKafkaListener")
    public void consumeBatchExceptionLogs(
            @Payload List<ExceptionLog> exceptionLogs,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("=== EXCEPTION CONSUMER CALLED ===");
            logger.info("Received {} exception logs from topic {}", exceptionLogs.size(), topic);

            if (exceptionLogs == null || exceptionLogs.isEmpty()) {
                logger.warn("Received empty or null batch of exception logs");
                acknowledgment.acknowledge();
                return;
            }

            bufferLock.lock();
            try {
                // Add received records to buffer
                buffer.addAll(exceptionLogs);
                logger.info("Added {} records to buffer. Total in buffer: {}", 
                    exceptionLogs.size(), buffer.size());

                // Process batches of exactly batchSize records
                while (buffer.size() >= batchSize) {
                    // Extract exactly batchSize records from buffer
                    List<ExceptionLog> batchToSave = new ArrayList<>(buffer.subList(0, batchSize));
                    // Remove first batchSize records from buffer
                    buffer.subList(0, batchSize).clear();
                    
                    logger.info("=== SAVING BATCH OF EXACTLY {} EXCEPTION LOGS ===", batchSize);
                    logger.info("Buffer now has {} records remaining", buffer.size());
                    
                    // Save exactly batchSize records to database
                    exceptionLogRepository.saveAll(batchToSave);
                    logger.info("✅ Successfully saved exactly {} exception logs to database", batchSize);
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
            logger.info("Acknowledged {} exception logs", exceptionLogs.size());

        } catch (Exception e) {
            logger.error("Error processing batch of exception logs", e);
            logger.error("Exception details: {}", e.getMessage(), e);
            // Don't acknowledge on error - let Kafka retry
        }
    }
}

