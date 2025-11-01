package com.aigreentick.audit.config;

import org.springframework.stereotype.Component;

/**
 * Thread-local context to hold audit information (username, IP address)
 * This allows automatic database-level auditing to capture user context
 */
@Component
public class MongoAuditContext {

    private static final ThreadLocal<String> usernameContext = new ThreadLocal<>();
    private static final ThreadLocal<String> ipAddressContext = new ThreadLocal<>();

    public static void setUsername(String username) {
        usernameContext.set(username);
    }

    public static String getUsername() {
        return usernameContext.get();
    }

    public static void setIpAddress(String ipAddress) {
        ipAddressContext.set(ipAddress);
    }

    public static String getIpAddress() {
        return ipAddressContext.get();
    }

    public static void clear() {
        usernameContext.remove();
        ipAddressContext.remove();
    }
}

