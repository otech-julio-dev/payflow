package com.payflow.wallet.service;

import com.payflow.wallet.dto.request.TopUpRequest;
import com.payflow.wallet.dto.response.AccountResponse;
import com.payflow.wallet.dto.response.BalanceResponse;
import com.payflow.wallet.entity.Account;
import com.payflow.wallet.exception.AccountNotFoundException;
import com.payflow.wallet.exception.AccountSuspendedException;
import com.payflow.wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletService {

    private final AccountRepository accountRepository;

    public WalletService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // ── Obtener o crear cuenta ────────────────────────────────
    @Transactional
    public AccountResponse getOrCreateAccount(Long userId) {
        Account account = accountRepository.findByUserId(userId)
            .orElseGet(() -> createAccount(userId));
        return AccountResponse.from(account);
    }

    // ── Consultar saldo ───────────────────────────────────────
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        Account account = accountRepository.findByUserId(userId)
            .orElseThrow(() -> new AccountNotFoundException(userId));

        return new BalanceResponse(
            account.getAccountNumber(),
            account.getBalance(),
            "MXN",
            LocalDateTime.now()
        );
    }

    // ── Top-up ────────────────────────────────────────────────
    @Transactional
    public AccountResponse topUp(Long userId, TopUpRequest req) {
        Account account = accountRepository.findByUserIdWithLock(userId)
            .orElseGet(() -> createAccount(userId));

        if (!account.isActive()) {
            throw new AccountSuspendedException(account.getAccountNumber());
        }

        account.credit(req.amount());
        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    // ── Private ───────────────────────────────────────────────
    private Account createAccount(Long userId) {
        String accountNumber = "PF-" + String.format("%010d", userId)
                             + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Account newAccount = new Account(userId, accountNumber);
        return accountRepository.save(newAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return AccountResponse.from(account);
    }

    @Transactional
    public void internalDebit(Long userId, BigDecimal amount) {
        Account account = accountRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new AccountNotFoundException(userId));
        if (!account.isActive()) throw new AccountSuspendedException(account.getAccountNumber());
        account.debit(amount);
        accountRepository.save(account);
    }

    @Transactional
    public void internalCredit(Long userId, BigDecimal amount) {
        Account account = accountRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new AccountNotFoundException(userId));
        account.credit(amount);
        accountRepository.save(account);
    }
}