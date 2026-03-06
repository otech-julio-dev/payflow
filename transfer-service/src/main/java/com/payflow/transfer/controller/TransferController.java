package com.payflow.transfer.controller;

import com.payflow.transfer.dto.request.TransferRequest;
import com.payflow.transfer.dto.response.TransferResponse;
import com.payflow.transfer.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            Authentication auth,
            @Valid @RequestBody TransferRequest req,
            HttpServletRequest httpRequest) {

        Long userId    = extractUserId(auth);
        String token   = extractToken(httpRequest);
        return ResponseEntity.status(201)
                .body(transferService.transfer(userId, req, token));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TransferResponse>> myTransfers(Authentication auth) {
        return ResponseEntity.ok(transferService.getMyTransfers(extractUserId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(
            Authentication auth,
            @PathVariable Long id) {
        return ResponseEntity.ok(transferService.getById(id, extractUserId(auth)));
    }

    // ── Helpers ───────────────────────────────────────────────
    private Long extractUserId(Authentication auth) {
        Object credentials = auth.getCredentials();
        if (credentials instanceof Long)    return (Long) credentials;
        if (credentials instanceof Integer) return ((Integer) credentials).longValue();
        throw new IllegalStateException("No se pudo extraer userId");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ")
            ? header.substring(7) : "";
    }
}