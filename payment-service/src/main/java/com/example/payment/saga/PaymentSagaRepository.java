package com.example.payment.saga;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSagaRepository extends JpaRepository<PaymentSaga, UUID> {
    Optional<PaymentSaga> findByPaymentId(UUID paymentId);
}
