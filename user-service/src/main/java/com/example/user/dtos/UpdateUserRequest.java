package com.example.user.dtos;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String email,
        String role
) {}