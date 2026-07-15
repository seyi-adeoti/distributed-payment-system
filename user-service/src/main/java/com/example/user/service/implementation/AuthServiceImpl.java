package com.example.user.service.implementation;

import com.example.common.exception.DuplicateResourceException;
import com.example.common.exception.ResourceNotFoundException;
import com.example.user.dtos.AuthResponse;
import com.example.user.dtos.LoginRequest;
import com.example.user.dtos.ResetPasswordRequest;
import com.example.user.dtos.SignupRequest;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import com.example.user.service.abstraction.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.module.ResolutionException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TwoFactorAuthService twoFactorAuthService;



    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUserName(request.userName())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .userName(request.userName())
                .email(request.email())
                .dob(request.dob())
                .password(passwordEncoder.encode(request.password()))
                .role("USER") // Default role
                .isEnabled(true)
                .isUsing2FA(false)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, false);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.userName())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        //Verify Password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // 2. Check if 2FA is enabled
        if (user.isUsing2FA()) {
            // If 2FA is enabled, we DO NOT return tokens yet.
            // We tell the frontend to show the 2FA input screen.
            return new AuthResponse(null, null, true);
        }

        // 3. Generate Tokens if 2FA is not enabled
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, false);
    }

    @Override
    public AuthResponse verify2FA(String userName, String code) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify the TOTP code against the user's stored secret
        boolean isValid = twoFactorAuthService.verifyCode(user.getSecret(), code);

        if (!isValid) {
            throw new RuntimeException("Invalid 2FA code");
        }

        // Code is valid, now issue the tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, false);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        final String userName = jwtService.extractUsername(refreshToken);

        if (userName == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Validate the refresh token (checks signature and expiration)
        if (!jwtService.isTokenValid(refreshToken, user.getUserName())) {
            throw new RuntimeException("Refresh token is expired or invalid");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken, false);
    }

    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // 1. Generate a secure, random token
            String token = UUID.randomUUID().toString();

            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

            userRepository.save(user);

            //Send Email (Mocked for now)


            String resetLink = "http://localhost:8081/api/v1/auth/reset-password?token=" + token;
            System.out.println("========================================");
            System.out.println("PASSWORD RESET LINK FOR " + email + ":");
            System.out.println(resetLink);
            System.out.println("========================================");
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }
}
