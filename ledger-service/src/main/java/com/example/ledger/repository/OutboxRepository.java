package com.example.ledger.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ledger.entity.OutboxEvent;
import com.example.ledger.entity.OutboxStatus;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
