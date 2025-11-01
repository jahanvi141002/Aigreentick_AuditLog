package com.aigreentick.audit.controller;

import com.aigreentick.audit.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example of integrating audit logging in an Invoice API
 * Shows how to log different operations with custom descriptions
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final Map<String, Map<String, Object>> invoiceStore = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final AuditLogService auditLogService;

    @Autowired
    public InvoiceController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createInvoice(
            @RequestBody Map<String, Object> invoice,
            HttpServletRequest request) {
        try {
            String invoiceId = UUID.randomUUID().toString();
            invoice.put("id", invoiceId);
            invoice.put("status", "DRAFT");
            
            invoiceStore.put(invoiceId, invoice);
            
            String newValueJson = objectMapper.writeValueAsString(invoice);
            String ipAddress = getClientIpAddress(request);
            String username = getCurrentUser(request);
            
            // Log with custom description
            auditLogService.log(
                username,
                "Invoice",
                invoiceId,
                "CREATE",
                ipAddress
            );
            
            // Or use logCreate for more details
            auditLogService.logCreate(
                username,
                "Invoice",
                invoiceId,
                newValueJson,
                ipAddress
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveInvoice(
            @PathVariable String id,
            HttpServletRequest request) {
        try {
            Map<String, Object> invoice = invoiceStore.get(id);
            if (invoice == null) {
                return ResponseEntity.notFound().build();
            }
            
            String oldStatus = (String) invoice.get("status");
            invoice.put("status", "APPROVED");
            
            String ipAddress = getClientIpAddress(request);
            String username = getCurrentUser(request);
            
            // Log status change with custom description
            String description = String.format("Invoice status changed from %s to APPROVED", oldStatus);
            auditLogService.log(
                username,
                "Invoice",
                id,
                "APPROVE",
                ipAddress
            );
            
            return ResponseEntity.ok(invoice);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectInvoice(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        try {
            Map<String, Object> invoice = invoiceStore.get(id);
            if (invoice == null) {
                return ResponseEntity.notFound().build();
            }
            
            invoice.put("status", "REJECTED");
            if (reason != null) {
                invoice.put("rejectionReason", reason);
            }
            
            String ipAddress = getClientIpAddress(request);
            String username = getCurrentUser(request);
            
            // Log with description containing rejection reason
            String description = reason != null ? "Rejected: " + reason : "Invoice rejected";
            auditLogService.log(
                username,
                "Invoice",
                id,
                "REJECT",
                ipAddress
            );
            
            return ResponseEntity.ok(invoice);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    private String getCurrentUser(HttpServletRequest request) {
        String username = request.getHeader("X-Username");
        return username != null && !username.isEmpty() ? username : "system";
    }
}

