package com.example.user.dtos;

import java.time.LocalDateTime;

public record CreateUserRequest(String firstName, String lastName, String userName, String email, String role, LocalDateTime dob) {}

