package com.example.user.dtos;

public record LoginRequest(String userName, String password, String code) {} // 'code' is for 2FA
