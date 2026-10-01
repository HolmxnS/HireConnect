package com.project.HireConnect.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String token;
    
    private String email;
    
    @Field("user_type")
    private String userType;
    
    @Field("expiry_date")
    private LocalDateTime expiryDate;
    
    private boolean used = false;
    
    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public PasswordResetToken(String token, String email, String userType) {
        this.token = token;
        this.email = email;
        this.userType = userType;
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.used = false;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}