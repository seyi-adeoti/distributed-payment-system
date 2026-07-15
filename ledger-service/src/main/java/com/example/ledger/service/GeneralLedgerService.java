package com.example.ledger.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeneralLedgerService {

    public void postIntraWalletTransfer(BigDecimal amount, String reference, String senderWalletId, String receiverWalletId) {
        // Stub for actual ledger posting logic.
        // In a real system, this would write to the double-entry accounting tables.
        // e.g. DEBIT sender liability account, CREDIT receiver liability account.
        log.info("Posting intra-wallet transfer to GL. Ref: {}, Amount: {}, Sender: {}, Receiver: {}",
                reference, amount, senderWalletId, receiverWalletId);
    }
}
