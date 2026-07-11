package com.example.payment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.payment.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
