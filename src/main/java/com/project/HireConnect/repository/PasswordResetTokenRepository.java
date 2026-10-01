package com.project.HireConnect.repository;

import com.project.HireConnect.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByEmail(String email);
    
    void deleteByEmail(String email);
    
    @Query(value = "{ $or: [ { 'expiry_date': { $lt: new Date() } }, { 'used': true } ] }", delete = true)
    void deleteExpiredOrUsedTokens();
}