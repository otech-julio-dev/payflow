package com.payflow.transaction.service;

import com.payflow.transaction.document.Transaction;
import com.payflow.transaction.dto.request.CreateTransactionRequest;
import com.payflow.transaction.dto.response.PageResponse;
import com.payflow.transaction.dto.response.TransactionResponse;
import com.payflow.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // ── Crear transacción (llamado internamente) ───────────────
    public TransactionResponse create(CreateTransactionRequest req) {
        Transaction transaction = switch (req.type()) {
            case "CREDIT" -> Transaction.credit(
                req.userId(), req.counterpartyUserId(),
                req.accountNumber(), req.counterpartyAccountNumber(),
                req.amount(), req.balanceAfter(),
                req.description(), req.referenceId()
            );
            case "DEBIT" -> Transaction.debit(
                req.userId(), req.counterpartyUserId(),
                req.accountNumber(), req.counterpartyAccountNumber(),
                req.amount(), req.balanceAfter(),
                req.description(), req.referenceId()
            );
            case "TOPUP" -> Transaction.topup(
                req.userId(), req.accountNumber(),
                req.amount(), req.balanceAfter(),
                req.description()
            );
            default -> throw new IllegalArgumentException(
                "Tipo de transacción inválido: " + req.type());
        };

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    // ── Historial paginado ────────────────────────────────────
    public PageResponse<TransactionResponse> getMyTransactions(
            Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> result =
            transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return new PageResponse<>(
            result.getContent().stream()
                  .map(TransactionResponse::from)
                  .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isLast()
        );
    }

    // ── Detalle de una transacción ────────────────────────────
    public TransactionResponse getById(String id, Long userId) {
        Transaction t = transactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        if (!t.getUserId().equals(userId)) {
            throw new RuntimeException("No tienes acceso a esta transacción");
        }
        return TransactionResponse.from(t);
    }
}