package com.example.user.dtos;

import java.time.LocalDateTime;

// 1. Authentication Requests
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String firstName;
    private String lastName;
    private String userName;
    private String email;
    private String password;
    private LocalDateTime dob;
}