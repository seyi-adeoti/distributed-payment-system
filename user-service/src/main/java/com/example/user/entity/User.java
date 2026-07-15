package com.example.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id ;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private  String lastName;

    private  String middleName;
    @Column(nullable = false)
    public String userName;

    @Column(nullable = false)
    public String email;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime dob;

    private String role;

    private  String password;

    private boolean isEnabled =false;

    private boolean isUsing2FA = false;
    private String secret;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;


}
