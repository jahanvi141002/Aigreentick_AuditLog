package com.aigreentick.audit.repository;

import com.aigreentick.audit.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    // All CRUD operations are automatically audited at database level
    // No need to manually add audit logging in service layer
    
}

