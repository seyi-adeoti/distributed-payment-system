package com.example.ledger.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ledger.entity.ProcessedEvent;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByReference(String reference);
}
