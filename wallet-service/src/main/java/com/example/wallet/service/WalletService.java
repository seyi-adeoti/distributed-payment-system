package com.example.wallet.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.exception.InsufficientFundsException;
import com.example.common.exception.ResourceNotFoundException;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.entity.WalletTransaction.TransactionType;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

        private final WalletRepository walletRepository;
        private final WalletTransactionRepository transactionRepository;
        private final com.example.wallet.service.implementation.AmlValidator amlValidator;

        @Transactional
        public void debitForPayment(UUID walletId, BigDecimal amount,
                        String reference, String narration) {

                Wallet wallet = walletRepository.findById(walletId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

                if (!"TIER_1".equals(wallet.getKycTier())) {
                        throw new com.example.common.exception.KycLimitExceededException(
                                        "Wallet " + walletId + " is not TIER_1. KYC upgrade required.");
                }

                if (!amlValidator.isTransactionClean(amount)) {
                        throw new RuntimeException("Transaction flagged by AML. Limit exceeded.");
                }

                if (wallet.getBalance().compareTo(amount) < 0) {
                        throw new InsufficientFundsException(walletId.toString(), amount.toPlainString());
                }

                wallet.setBalance(wallet.getBalance().subtract(amount));
                walletRepository.save(wallet);

                transactionRepository.save(WalletTransaction.builder()
                                .walletId(walletId)
                                .type(TransactionType.DEBIT)
                                .amount(amount)
                                .reference(reference)
                                .narration(narration)
                                .build());

                log.info("Debited {} from wallet {}. Ref: {}", amount, walletId, reference);
        }

        @Transactional
        public void creditForPayment(UUID walletId, BigDecimal amount,
                        String reference, String narration) {

                Wallet wallet = walletRepository.findById(walletId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

                wallet.setBalance(wallet.getBalance().add(amount));
                walletRepository.save(wallet);

                transactionRepository.save(WalletTransaction.builder()
                                .walletId(walletId)
                                .type(TransactionType.CREDIT)
                                .amount(amount)
                                .reference(reference)
                                .narration(narration)
                                .build());

                log.info("Credited {} to wallet {}. Ref: {}", amount, walletId, reference);
        }

        @Transactional
        public void reverseDebit(UUID walletId, BigDecimal amount, String reference) {

                Wallet wallet = walletRepository.findById(walletId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

                wallet.setBalance(wallet.getBalance().add(amount));
                walletRepository.save(wallet);

                transactionRepository.save(WalletTransaction.builder()
                                .walletId(walletId)
                                .type(TransactionType.REVERSAL)
                                .amount(amount)
                                .reference(reference + "-REV")
                                .narration("Reversal of " + reference)
                                .build());

                log.info("Reversed debit of {} for wallet {}. Ref: {}", amount, walletId, reference);
        }

        @Transactional
        public void debitForReversal(UUID walletId, BigDecimal amount, String reference, String narration) {

                Wallet wallet = walletRepository.findById(walletId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

                if (wallet.getBalance().compareTo(amount) < 0) {
                        throw new InsufficientFundsException(walletId.toString(), amount.toPlainString());
                }

                wallet.setBalance(wallet.getBalance().subtract(amount));
                walletRepository.save(wallet);

                transactionRepository.save(WalletTransaction.builder()
                                .walletId(walletId)
                                .type(TransactionType.DEBIT)
                                .amount(amount)
                                .reference(reference)
                                .narration(narration)
                                .build());

                log.info("Debited {} from wallet {} for reversal. Ref: {}", amount, walletId, reference);
        }

        public BigDecimal getBalance(UUID walletId) {
                Wallet wallet = walletRepository.findById(walletId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));
                return wallet.getBalance();
        }
}
