package com.example.payment.dlq;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetterEvent, UUID> {
}
