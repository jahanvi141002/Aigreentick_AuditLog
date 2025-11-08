package com.aigreentick.audit.exception;

import com.aigreentick.audit.model.ExceptionLog;
import com.aigreentick.audit.service.ExceptionLogKafkaProducer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExceptionLogKafkaProducer exceptionLogKafkaProducer;

    @Autowired
    public GlobalExceptionHandler(ExceptionLogKafkaProducer exceptionLogKafkaProducer) {
        this.exceptionLogKafkaProducer = exceptionLogKafkaProducer;
    }

    /**
     * Handle all exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(
            Exception ex,
            HttpServletRequest request,
            WebRequest webRequest) {
        
        logger.error("Exception occurred: {}", ex.getMessage(), ex);

        // Extract request details
        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        String ipAddress = getClientIpAddress(request);
        
        // Extract class and method name from stack trace
        String className = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getClassName() : "Unknown";
        String methodName = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getMethodName() : "Unknown";

        // Extract user context from request (if available)
        String username = (String) request.getAttribute("username");
        String userId = (String) request.getAttribute("userId");
        String organizationId = (String) request.getAttribute("organizationId");

        // Create exception log
        ExceptionLog exceptionLog = new ExceptionLog(ex);
        exceptionLog.setClassName(className);
        exceptionLog.setMethodName(methodName);
        exceptionLog.setRequestUrl(requestUrl);
        exceptionLog.setRequestMethod(requestMethod);
        exceptionLog.setUsername(username);
        exceptionLog.setUserId(userId);
        exceptionLog.setOrganizationId(organizationId);
        exceptionLog.setIpAddress(ipAddress);
        exceptionLog.setRequestParameters(getRequestParameters(request));
        exceptionLog.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionLog.setDescription("Exception occurred during request processing");

        // Send exception log to Kafka
        try {
            exceptionLogKafkaProducer.sendExceptionLog(exceptionLog);
        } catch (Exception e) {
            logger.error("Failed to send exception log to Kafka: {}", e.getMessage(), e);
        }

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", ex.getMessage());
        response.put("path", requestUrl);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle specific runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        logger.error("RuntimeException occurred: {}", ex.getMessage(), ex);

        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        String ipAddress = getClientIpAddress(request);
        String className = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getClassName() : "Unknown";
        String methodName = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getMethodName() : "Unknown";

        String username = (String) request.getAttribute("username");
        String userId = (String) request.getAttribute("userId");
        String organizationId = (String) request.getAttribute("organizationId");

        ExceptionLog exceptionLog = new ExceptionLog(ex);
        exceptionLog.setClassName(className);
        exceptionLog.setMethodName(methodName);
        exceptionLog.setRequestUrl(requestUrl);
        exceptionLog.setRequestMethod(requestMethod);
        exceptionLog.setUsername(username);
        exceptionLog.setUserId(userId);
        exceptionLog.setOrganizationId(organizationId);
        exceptionLog.setIpAddress(ipAddress);
        exceptionLog.setRequestParameters(getRequestParameters(request));
        exceptionLog.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionLog.setDescription("RuntimeException occurred during request processing");

        try {
            exceptionLogService.createExceptionLog(exceptionLog);
        } catch (Exception e) {
            logger.error("Failed to save exception log: {}", e.getMessage(), e);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Runtime Exception");
        response.put("message", ex.getMessage());
        response.put("path", requestUrl);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        logger.error("IllegalArgumentException occurred: {}", ex.getMessage(), ex);

        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        String ipAddress = getClientIpAddress(request);
        String className = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getClassName() : "Unknown";
        String methodName = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getMethodName() : "Unknown";

        String username = (String) request.getAttribute("username");
        String userId = (String) request.getAttribute("userId");
        String organizationId = (String) request.getAttribute("organizationId");

        ExceptionLog exceptionLog = new ExceptionLog(ex);
        exceptionLog.setClassName(className);
        exceptionLog.setMethodName(methodName);
        exceptionLog.setRequestUrl(requestUrl);
        exceptionLog.setRequestMethod(requestMethod);
        exceptionLog.setUsername(username);
        exceptionLog.setUserId(userId);
        exceptionLog.setOrganizationId(organizationId);
        exceptionLog.setIpAddress(ipAddress);
        exceptionLog.setRequestParameters(getRequestParameters(request));
        exceptionLog.setHttpStatus(HttpStatus.BAD_REQUEST.value());
        exceptionLog.setDescription("IllegalArgumentException occurred - invalid argument provided");

        try {
            exceptionLogService.createExceptionLog(exceptionLog);
        } catch (Exception e) {
            logger.error("Failed to save exception log: {}", e.getMessage(), e);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());
        response.put("path", requestUrl);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {
        
        logger.error("NullPointerException occurred: {}", ex.getMessage(), ex);

        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        String ipAddress = getClientIpAddress(request);
        String className = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getClassName() : "Unknown";
        String methodName = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0].getMethodName() : "Unknown";

        String username = (String) request.getAttribute("username");
        String userId = (String) request.getAttribute("userId");
        String organizationId = (String) request.getAttribute("organizationId");

        ExceptionLog exceptionLog = new ExceptionLog(ex);
        exceptionLog.setClassName(className);
        exceptionLog.setMethodName(methodName);
        exceptionLog.setRequestUrl(requestUrl);
        exceptionLog.setRequestMethod(requestMethod);
        exceptionLog.setUsername(username);
        exceptionLog.setUserId(userId);
        exceptionLog.setOrganizationId(organizationId);
        exceptionLog.setIpAddress(ipAddress);
        exceptionLog.setRequestParameters(getRequestParameters(request));
        exceptionLog.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionLog.setDescription("NullPointerException occurred - null reference accessed");

        try {
            exceptionLogService.createExceptionLog(exceptionLog);
        } catch (Exception e) {
            logger.error("Failed to save exception log: {}", e.getMessage(), e);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Null Pointer Exception");
        response.put("message", "A null reference was accessed");
        response.put("path", requestUrl);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method to extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Helper method to get request parameters as string
     */
    private String getRequestParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) {
            return null;
        }
        
        StringBuilder params = new StringBuilder();
        parameterMap.forEach((key, values) -> {
            if (params.length() > 0) {
                params.append("&");
            }
            params.append(key).append("=");
            if (values.length > 0) {
                params.append(String.join(",", values));
            }
        });
        
        return params.toString();
    }
}

