package com.example.user.dtos;

public record AuthResponse(String accessToken, String refreshToken, boolean requires2FA) {}

