package com.example.user.dtos;

import java.time.LocalDateTime;

// 1. Authentication Requests
public record SignupRequest(String firstName, String lastName, String userName, String email, String password, LocalDateTime dob) {}

