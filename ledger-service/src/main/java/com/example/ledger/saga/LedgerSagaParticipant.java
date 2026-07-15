package com.example.ledger.saga;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.event.LedgerPostFailedEvent;
import com.example.common.event.LedgerPostRequestedEvent;
import com.example.common.event.LedgerPostedEvent;
import com.example.ledger.entity.ProcessedEvent;
import com.example.ledger.repository.ProcessedEventRepository;
import com.example.ledger.service.GeneralLedgerService;
import com.example.ledger.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerSagaParticipant {

    private final GeneralLedgerService glService;
    private final OutboxService outboxService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void handleLedgerPostRequested(LedgerPostRequestedEvent event) {

        if (processedEventRepository.existsByReference(event.getReference())) {
            log.warn("Ledger already posted for ref: {}. Skipping.", event.getReference());
            return;
        }

        log.info("Posting ledger entries for payment: {}", event.getPaymentId());

        try {
            // Post double-entry:
            // DEBIT  2001 (Customer Wallet Liability — sender)
            // CREDIT 2001 (Customer Wallet Liability — receiver)
            glService.postIntraWalletTransfer(
                event.getAmount(),
                event.getReference(),
                event.getSenderWalletId().toString(),
                event.getReceiverWalletId().toString()
            );

            processedEventRepository.save(ProcessedEvent.builder()
                    .reference(event.getReference())
                    .eventType("LedgerPostRequested")
                    .processedAt(LocalDateTime.now())
                    .build());

            outboxService.write(
                event.getPaymentId(),
                "Ledger",
                "LedgerPosted",
                LedgerPostedEvent.builder()
                    .paymentId(event.getPaymentId())
                    .reference(event.getReference())
                    .debitAccountCode("2001")
                    .creditAccountCode("2001")
                    .amount(event.getAmount())
                    .occurredAt(LocalDateTime.now())
                    .build()
            );

            log.info("Ledger posted for payment: {}", event.getPaymentId());

        } catch (Exception e) {
            log.error("Ledger post failed for payment: {} Error: {}",
                event.getPaymentId(), e.getMessage());

            outboxService.write(
                event.getPaymentId(),
                "Ledger",
                "LedgerPostFailed",
                LedgerPostFailedEvent.builder()
                    .paymentId(event.getPaymentId())
                    .reference(event.getReference())
                    .reason(e.getMessage())
                    .occurredAt(LocalDateTime.now())
                    .build()
            );
        }
    }
}
