package com.aigreentick.audit.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Web filter to capture user context (username and IP) from HTTP requests
 * This allows database-level auditing to know who performed the operation
 */
@Component
@Order(1)
public class MongoAuditWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            // Extract username from header or use "anonymous"
            String username = request.getHeader("X-Username");
            if (username == null || username.isEmpty()) {
                username = "anonymous";
            }

            // Extract IP address
            String ipAddress = getClientIpAddress(request);

            // Set context for database-level auditing
            MongoAuditContext.setUsername(username);
            MongoAuditContext.setIpAddress(ipAddress);

            // Continue with the request
            filterChain.doFilter(request, response);

        } finally {
            // Clear context after request to avoid memory leaks
            MongoAuditContext.clear();
        }
    }

    /**
     * Extract client IP address from request
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
}

