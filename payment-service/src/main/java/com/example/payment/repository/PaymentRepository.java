package com.example.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByReference(String reference);
    Optional<Payment> findByReference(String reference);
}
    