package com.aigreentick.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "exception_logs")
public class ExceptionLog {

    @Id
    private String id;

    // Exception class name
    @Field(name = "exception_type")
    private String exceptionType;

    // Exception message
    @Field(name = "exception_message")
    private String exceptionMessage;

    // Full stack trace
    @Field(name = "stack_trace")
    private String stackTrace;

    // Where the exception occurred (class and method)
    @Field(name = "class_name")
    private String className;

    @Field(name = "method_name")
    private String methodName;

    // HTTP request details (if applicable)
    @Field(name = "request_url")
    private String requestUrl;

    @Field(name = "request_method")
    private String requestMethod;

    @Field(name = "request_parameters")
    private String requestParameters;

    // User context (if available)
    @Field(name = "username")
    private String username;

    @Field(name = "user_id")
    private String userId;

    @Field(name = "organization_id")
    private String organizationId;

    // IP address
    @Field(name = "ip_address")
    private String ipAddress;

    // Additional context or description
    @Field(name = "description")
    private String description;

    // When the exception occurred
    @Field(name = "timestamp")
    private LocalDateTime timestamp;

    // HTTP status code (if applicable)
    @Field(name = "http_status")
    private Integer httpStatus;

    // Default constructor
    public ExceptionLog() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with exception details
    public ExceptionLog(Exception exception) {
        this.exceptionType = exception.getClass().getName();
        this.exceptionMessage = exception.getMessage();
        this.stackTrace = getStackTraceAsString(exception);
        this.timestamp = LocalDateTime.now();
    }

    // Helper method to convert stack trace to string
    private String getStackTraceAsString(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(String requestParameters) {
        this.requestParameters = requestParameters;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    @Override
    public String toString() {
        return "ExceptionLog{" +
                "id='" + id + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", requestUrl='" + requestUrl + '\'' +
                ", username='" + username + '\'' +
                ", timestamp=" + timestamp +
                ", httpStatus=" + httpStatus +
                '}';
    }
}

