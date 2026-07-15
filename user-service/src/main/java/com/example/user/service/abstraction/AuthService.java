package com.example.user.service.abstraction;

import com.example.user.dtos.AuthResponse;
import com.example.user.dtos.LoginRequest;
import com.example.user.dtos.ResetPasswordRequest;
import com.example.user.dtos.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    AuthResponse verify2FA(String userName, String code);
}
