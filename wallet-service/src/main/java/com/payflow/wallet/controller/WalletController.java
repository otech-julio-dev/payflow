package com.payflow.wallet.controller;

import com.payflow.wallet.dto.request.InternalOperationRequest;
import org.springframework.web.bind.annotation.PathVariable;
import com.payflow.wallet.dto.request.TopUpRequest;
import com.payflow.wallet.dto.response.AccountResponse;
import com.payflow.wallet.dto.response.BalanceResponse;
import com.payflow.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(walletService.getOrCreateAccount(userId));
    }

    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> getBalance(Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/topup")
    public ResponseEntity<AccountResponse> topUp(
            Authentication auth,
            @Valid @RequestBody TopUpRequest req) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(walletService.topUp(userId, req));
    }

    // El credentials guarda el userId desde el JwtAuthFilter
    private Long extractUserId(Authentication auth) {
        Object credentials = auth.getCredentials();
        if (credentials instanceof Long) {
            return (Long) credentials;
        }
        if (credentials instanceof Integer) {
            return ((Integer) credentials).longValue();
        }
        throw new IllegalStateException("No se pudo extraer el userId del token");
    }

    @GetMapping("/by-account/{accountNumber}")
    public ResponseEntity<AccountResponse> getByAccountNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(walletService.getByAccountNumber(accountNumber));
    }

    @PostMapping("/internal/debit")
    public ResponseEntity<Void> internalDebit(
            @RequestBody InternalOperationRequest req) {
        walletService.internalDebit(req.userId(), req.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/credit")
    public ResponseEntity<Void> internalCredit(
            @RequestBody InternalOperationRequest req) {
        walletService.internalCredit(req.userId(), req.amount());
        return ResponseEntity.ok().build();
    }
}