package com.aigreentick.audit.repository;

import com.aigreentick.audit.model.ExceptionLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExceptionLogRepository extends MongoRepository<ExceptionLog, String> {

    // Find by exception type
    List<ExceptionLog> findByExceptionType(String exceptionType);

    // Find by username
    List<ExceptionLog> findByUsername(String username);

    // Find by user ID
    List<ExceptionLog> findByUserId(String userId);

    // Find by organization ID
    List<ExceptionLog> findByOrganizationId(String organizationId);

    // Find by class name
    List<ExceptionLog> findByClassName(String className);

    // Find by HTTP status
    List<ExceptionLog> findByHttpStatus(Integer httpStatus);

    // Find by timestamp range
    List<ExceptionLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Find by exception type and timestamp range
    List<ExceptionLog> findByExceptionTypeAndTimestampBetween(String exceptionType, LocalDateTime start, LocalDateTime end);

    // Find by username and timestamp range
    List<ExceptionLog> findByUsernameAndTimestampBetween(String username, LocalDateTime start, LocalDateTime end);

    // Find by organization ID and timestamp range
    List<ExceptionLog> findByOrganizationIdAndTimestampBetween(String organizationId, LocalDateTime start, LocalDateTime end);
}

