package com.aigreentick.audit.controller;

import com.aigreentick.audit.model.User;
import com.aigreentick.audit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Demo controller showing database-level audit logging
 * All operations are automatically audited at the database level without manual code
 */
@RestController
@RequestMapping("/api/demo/users")
public class DatabaseAuditDemoController {

    @Autowired
    private UserRepository userRepository;

    /**
     * CREATE - Automatically audited by MongoAuditEventListener
     * No manual audit logging needed!
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Just save - audit happens automatically at database level
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * UPDATE - Automatically audited by MongoAuditEventListener
     * Old and new values are captured automatically!
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User updatedUser) {
        Optional<User> existingOpt = userRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existing = existingOpt.get();
        
        // Update fields
        if (updatedUser.getUsername() != null) {
            existing.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getEmail() != null) {
            existing.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getFullName() != null) {
            existing.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getRole() != null) {
            existing.setRole(updatedUser.getRole());
        }

        // Just save - audit happens automatically
        User saved = userRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE - Automatically audited by MongoAuditEventListener
     * Deleted document is captured automatically!
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // Just delete - audit happens automatically
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}

