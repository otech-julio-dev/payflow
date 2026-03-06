package com.payflow.transaction.controller;

import com.payflow.transaction.dto.request.CreateTransactionRequest;
import com.payflow.transaction.dto.response.PageResponse;
import com.payflow.transaction.dto.response.TransactionResponse;
import com.payflow.transaction.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ── Endpoint interno (sin JWT — llamado por transfer-service) ──
    @PostMapping("/internal")
    public ResponseEntity<TransactionResponse> createInternal(
            @RequestBody CreateTransactionRequest req) {
        return ResponseEntity.status(201)
                .body(transactionService.create(req));
    }

    // ── Historial paginado del usuario autenticado ────────────
    @GetMapping("/my")
    public ResponseEntity<PageResponse<TransactionResponse>> myTransactions(
            Authentication auth,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(
                transactionService.getMyTransactions(userId, page, size));
    }

    // ── Detalle de una transacción ────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(
            Authentication auth,
            @PathVariable String id) {
        return ResponseEntity.ok(
                transactionService.getById(id, extractUserId(auth)));
    }

    private Long extractUserId(Authentication auth) {
        Object credentials = auth.getCredentials();
        if (credentials instanceof Long)    return (Long) credentials;
        if (credentials instanceof Integer) return ((Integer) credentials).longValue();
        throw new IllegalStateException("No se pudo extraer userId");
    }
}