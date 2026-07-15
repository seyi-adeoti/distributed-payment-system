package com.example.payment.saga;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaEventRepository extends JpaRepository<SagaEvent, UUID> {
}
