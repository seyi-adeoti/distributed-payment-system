package com.example.user.dtos;

public record ResetPasswordRequest(String token, String newPassword) {}
