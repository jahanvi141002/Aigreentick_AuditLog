package com.aigreentick.audit.config;

import org.springframework.stereotype.Component;

/**
 * Thread-local context to hold audit information (username, userId, organizationId, urlDomain, IP address)
 * This allows automatic database-level auditing to capture user context
 */
@Component
public class MongoAuditContext {

    private static final ThreadLocal<String> usernameContext = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdContext = new ThreadLocal<>();
    private static final ThreadLocal<String> organizationIdContext = new ThreadLocal<>();
    private static final ThreadLocal<String> urlDomainContext = new ThreadLocal<>();
    private static final ThreadLocal<String> ipAddressContext = new ThreadLocal<>();

    public static void setUsername(String username) {
        usernameContext.set(username);
    }

    public static String getUsername() {
        return usernameContext.get();
    }

    public static void setUserId(String userId) {
        userIdContext.set(userId);
    }

    public static String getUserId() {
        return userIdContext.get();
    }

    public static void setOrganizationId(String organizationId) {
        organizationIdContext.set(organizationId);
    }

    public static String getOrganizationId() {
        return organizationIdContext.get();
    }

    public static void setUrlDomain(String urlDomain) {
        urlDomainContext.set(urlDomain);
    }

    public static String getUrlDomain() {
        return urlDomainContext.get();
    }

    public static void setIpAddress(String ipAddress) {
        ipAddressContext.set(ipAddress);
    }

    public static String getIpAddress() {
        return ipAddressContext.get();
    }

    public static void clear() {
        usernameContext.remove();
        userIdContext.remove();
        organizationIdContext.remove();
        urlDomainContext.remove();
        ipAddressContext.remove();
    }
}

