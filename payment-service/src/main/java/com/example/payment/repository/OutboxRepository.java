package com.example.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.payment.entity.OutboxEvent;
import com.example.payment.entity.OutboxStatus;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
