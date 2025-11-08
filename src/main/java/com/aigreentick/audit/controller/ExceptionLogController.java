package com.aigreentick.audit.controller;

import com.aigreentick.audit.model.ExceptionLog;
import com.aigreentick.audit.service.ExceptionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/exception-logs")
public class ExceptionLogController {

    private final ExceptionLogService exceptionLogService;

    @Autowired
    public ExceptionLogController(ExceptionLogService exceptionLogService) {
        this.exceptionLogService = exceptionLogService;
    }

    /**
     * Create a new exception log entry (for manual logging if needed)
     */
    @PostMapping
    public ResponseEntity<ExceptionLog> createExceptionLog(@RequestBody ExceptionLog exceptionLog) {
        ExceptionLog savedLog = exceptionLogService.createExceptionLog(exceptionLog);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLog);
    }

    /**
     * Get all exception logs
     */
    @GetMapping
    public ResponseEntity<List<ExceptionLog>> getAllExceptionLogs() {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getAllExceptionLogs();
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExceptionLog> getExceptionLogById(@PathVariable String id) {
        Optional<ExceptionLog> exceptionLog = exceptionLogService.getExceptionLogById(id);
        return exceptionLog.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get exception logs by exception type
     */
    @GetMapping("/type/{exceptionType}")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByExceptionType(@PathVariable String exceptionType) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByExceptionType(exceptionType);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByUsername(@PathVariable String username) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByUsername(username);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by user ID
     */
    @GetMapping("/user-id/{userId}")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByUserId(@PathVariable String userId) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByUserId(userId);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by organization ID
     */
    @GetMapping("/organization-id/{organizationId}")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByOrganizationId(@PathVariable String organizationId) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByOrganizationId(organizationId);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by class name
     */
    @GetMapping("/class/{className}")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByClassName(@PathVariable String className) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByClassName(className);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by HTTP status
     */
    @GetMapping("/http-status/{httpStatus}")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByHttpStatus(@PathVariable Integer httpStatus) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByHttpStatus(httpStatus);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByDateRange(start, end);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by exception type and date range
     */
    @GetMapping("/type/{exceptionType}/date-range")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByExceptionTypeAndDateRange(
            @PathVariable String exceptionType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByExceptionTypeAndDateRange(
                exceptionType, start, end);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by username and date range
     */
    @GetMapping("/username/{username}/date-range")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByUsernameAndDateRange(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByUsernameAndDateRange(
                username, start, end);
        return ResponseEntity.ok(exceptionLogs);
    }

    /**
     * Get exception logs by organization ID and date range
     */
    @GetMapping("/organization-id/{organizationId}/date-range")
    public ResponseEntity<List<ExceptionLog>> getExceptionLogsByOrganizationIdAndDateRange(
            @PathVariable String organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ExceptionLog> exceptionLogs = exceptionLogService.getExceptionLogsByOrganizationIdAndDateRange(
                organizationId, start, end);
        return ResponseEntity.ok(exceptionLogs);
    }
}

