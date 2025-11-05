package com.aigreentick.audit.config;

import com.aigreentick.audit.model.AuditLog;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${spring.kafka.consumer.fetch-min-bytes:1024}")
    private Integer fetchMinBytes;

    @Value("${spring.kafka.consumer.fetch-max-wait-ms:500}")
    private Integer fetchMaxWaitMs;

    @Value("${spring.kafka.producer.acks:1}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private Integer batchSize;

    @Value("${spring.kafka.producer.linger-ms:10}")
    private Integer lingerMs;

    @Value("${spring.kafka.producer.buffer-memory:33554432}")
    private Long bufferMemory;

    @Value("${spring.kafka.listener.concurrency:3}")
    private Integer concurrency;

    @Bean
    public ProducerFactory<String, AuditLog> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, AuditLog> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, AuditLog> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Add session timeout and heartbeat to help with group joining
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        // CRITICAL: Increase timeouts to handle coordinator timeout issues
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000); // Increase request timeout
        props.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000); // Increase default API timeout
        
        // Reduce initial rebalance delay for faster partition assignment
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 30000); // Refresh metadata every 30 seconds
        
        // Note: GROUP_INITIAL_REBALANCE_DELAY_MS is a broker config, not consumer config
        // But we can reduce consumer-side delays
        
        // Allow auto topic creation (if needed)
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);
        
        // Force consumer to start consuming immediately
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "audit-consumer-" + System.currentTimeMillis());
        
        // CRITICAL: Increase reconnection delays to handle coordinator connection issues
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 100);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 1000);
        
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuditLog.class.getName());
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditLog> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AuditLog> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);
        
        // Disable idle events - not needed for simple consumer
        // factory.getContainerProperties().setIdleEventInterval(5000L);
        
        // Set sync commits to ensure offset commits happen synchronously
        factory.getContainerProperties().setSyncCommits(true);
        
        // Log container lifecycle events
        factory.getContainerProperties().setLogContainerConfig(true);
        
        // Force immediate start - don't wait for group rebalance delay
        factory.setAutoStartup(true);
        
        // Add error handler to log deserialization errors
        CommonErrorHandler errorHandler = new CommonLoggingErrorHandler();
        factory.setCommonErrorHandler(errorHandler);
        
        // Add listener to track partition assignment - CRITICAL for debugging
        factory.getContainerProperties().setConsumerRebalanceListener(new ConsumerRebalanceListener() {
            private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KafkaConfig.class);
            
            @Override
            public void onPartitionsRevoked(java.util.Collection<TopicPartition> partitions) {
                logger.error("=== REBALANCE: Partitions revoked: {} ===", partitions);
                if (partitions == null || partitions.isEmpty()) {
                    logger.warn("=== REBALANCE: No partitions to revoke (this is normal during startup) ===");
                }
            }
            
            @Override
            public void onPartitionsAssigned(java.util.Collection<TopicPartition> partitions) {
                logger.error("=== REBALANCE: Partitions assigned: {} ===", partitions);
                if (partitions != null && !partitions.isEmpty()) {
                    logger.error("=== REBALANCE: SUCCESS! Assigned {} partition(s) ===", partitions.size());
                    for (TopicPartition partition : partitions) {
                        logger.error("=== REBALANCE: Partition {} assigned to consumer ===", partition);
                    }
                } else {
                    logger.error("=== REBALANCE: FAILED! No partitions assigned! This is a problem! ===");
                }
            }
        });
        
        return factory;
    }
}

