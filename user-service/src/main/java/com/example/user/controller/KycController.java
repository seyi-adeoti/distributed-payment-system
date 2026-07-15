package com.example.user.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user.service.implementation.KycService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping("/upgrade/{userId}")
    public ResponseEntity<String> upgradeUser(@PathVariable UUID userId) {
        kycService.upgradeUserToTier1(userId);
        return ResponseEntity.ok("User successfully upgraded to TIER_1 and event emitted.");
    }
}
