package com.example.payment.repository;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.example.common.event.PaymentInitiatedEvent;

public interface PaymentRepository extends JpaRepository<PaymentInitiatedEvent, UUID> {
    boolean existsByReference(String reference);
    PaymentInitiatedEvent findByReference(String reference);
  
}
