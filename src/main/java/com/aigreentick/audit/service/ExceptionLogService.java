package com.aigreentick.audit.service;

import com.aigreentick.audit.model.ExceptionLog;
import com.aigreentick.audit.repository.ExceptionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExceptionLogService {

    private final ExceptionLogRepository exceptionLogRepository;

    @Autowired
    public ExceptionLogService(ExceptionLogRepository exceptionLogRepository) {
        this.exceptionLogRepository = exceptionLogRepository;
    }

    /**
     * Create a new exception log entry
     */
    public ExceptionLog createExceptionLog(ExceptionLog exceptionLog) {
        if (exceptionLog.getTimestamp() == null) {
            exceptionLog.setTimestamp(LocalDateTime.now());
        }
        return exceptionLogRepository.save(exceptionLog);
    }

    /**
     * Log an exception with all details
     */
    public ExceptionLog logException(Exception exception, String className, String methodName,
                                     String requestUrl, String requestMethod, String username,
                                     String userId, String organizationId, String ipAddress) {
        ExceptionLog exceptionLog = new ExceptionLog(exception);
        exceptionLog.setClassName(className);
        exceptionLog.setMethodName(methodName);
        exceptionLog.setRequestUrl(requestUrl);
        exceptionLog.setRequestMethod(requestMethod);
        exceptionLog.setUsername(username);
        exceptionLog.setUserId(userId);
        exceptionLog.setOrganizationId(organizationId);
        exceptionLog.setIpAddress(ipAddress);
        return createExceptionLog(exceptionLog);
    }

    /**
     * Get all exception logs
     */
    public List<ExceptionLog> getAllExceptionLogs() {
        return exceptionLogRepository.findAll();
    }

    /**
     * Get exception log by ID
     */
    public Optional<ExceptionLog> getExceptionLogById(String id) {
        return exceptionLogRepository.findById(id);
    }

    /**
     * Get exception logs by exception type
     */
    public List<ExceptionLog> getExceptionLogsByExceptionType(String exceptionType) {
        return exceptionLogRepository.findByExceptionType(exceptionType);
    }

    /**
     * Get exception logs by username
     */
    public List<ExceptionLog> getExceptionLogsByUsername(String username) {
        return exceptionLogRepository.findByUsername(username);
    }

    /**
     * Get exception logs by user ID
     */
    public List<ExceptionLog> getExceptionLogsByUserId(String userId) {
        return exceptionLogRepository.findByUserId(userId);
    }

    /**
     * Get exception logs by organization ID
     */
    public List<ExceptionLog> getExceptionLogsByOrganizationId(String organizationId) {
        return exceptionLogRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get exception logs by class name
     */
    public List<ExceptionLog> getExceptionLogsByClassName(String className) {
        return exceptionLogRepository.findByClassName(className);
    }

    /**
     * Get exception logs by HTTP status
     */
    public List<ExceptionLog> getExceptionLogsByHttpStatus(Integer httpStatus) {
        return exceptionLogRepository.findByHttpStatus(httpStatus);
    }

    /**
     * Get exception logs by timestamp range
     */
    public List<ExceptionLog> getExceptionLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return exceptionLogRepository.findByTimestampBetween(start, end);
    }

    /**
     * Get exception logs by exception type and date range
     */
    public List<ExceptionLog> getExceptionLogsByExceptionTypeAndDateRange(String exceptionType,
                                                                          LocalDateTime start,
                                                                          LocalDateTime end) {
        return exceptionLogRepository.findByExceptionTypeAndTimestampBetween(exceptionType, start, end);
    }

    /**
     * Get exception logs by username and date range
     */
    public List<ExceptionLog> getExceptionLogsByUsernameAndDateRange(String username,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end) {
        return exceptionLogRepository.findByUsernameAndTimestampBetween(username, start, end);
    }

    /**
     * Get exception logs by organization ID and date range
     */
    public List<ExceptionLog> getExceptionLogsByOrganizationIdAndDateRange(String organizationId,
                                                                            LocalDateTime start,
                                                                            LocalDateTime end) {
        return exceptionLogRepository.findByOrganizationIdAndTimestampBetween(organizationId, start, end);
    }
}

