package com.aigreentick.audit;

import com.aigreentick.audit.model.AuditLog;
import com.aigreentick.audit.model.User;
import com.aigreentick.audit.repository.AuditLogRepository;
import com.aigreentick.audit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class KafkaAuditIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateUserGeneratesAuditLog() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole("USER");

        User saved = userRepository.save(user);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.stream()
                    .anyMatch(log -> log.getAction().equals("CREATE") &&
                            log.getEntityName().equals("User") &&
                            log.getEntityId().equals(saved.getId()))).isTrue();
        });
    }

    @Test
    void testUpdateUserGeneratesAuditLog() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole("USER");
        User saved = userRepository.save(user);

        saved.setEmail("updated@example.com");
        userRepository.save(saved);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityId("User", saved.getId());
            assertThat(auditLogs).isNotEmpty();
            assertThat(auditLogs.stream()
                    .anyMatch(log -> log.getAction().equals("UPDATE"))).isTrue();
        });
    }

    @Test
    void testDeleteUserGeneratesAuditLog() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole("USER");
        User saved = userRepository.save(user);

        String userId = saved.getId();
        userRepository.deleteById(userId);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findByEntityName("User");
            assertThat(auditLogs.stream()
                    .anyMatch(log -> log.getAction().equals("DELETE") &&
                            log.getEntityId().equals(userId))).isTrue();
        });
    }

    @Test
    void testBatchProcessing() {
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setUsername("batchuser" + i);
            user.setEmail("batch" + i + "@example.com");
            user.setFullName("Batch User " + i);
            user.setRole("USER");
            userRepository.save(user);
        }

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLog> auditLogs = auditLogRepository.findByAction("CREATE");
            assertThat(auditLogs.size()).isGreaterThanOrEqualTo(5);
        });
    }
}

