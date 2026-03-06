package com.payflow.transaction.dto.response;

import com.payflow.transaction.document.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    String         id,
    Long           userId,
    Long           counterpartyUserId,
    String         accountNumber,
    String         counterpartyAccountNumber,
    String         type,
    BigDecimal     amount,
    BigDecimal     balanceAfter,
    String         description,
    String         referenceId,
    String         status,
    LocalDateTime  createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
            t.getId(),
            t.getUserId(),
            t.getCounterpartyUserId(),
            t.getAccountNumber(),
            t.getCounterpartyAccountNumber(),
            t.getType().name(),
            t.getAmount(),
            t.getBalanceAfter(),
            t.getDescription(),
            t.getReferenceId(),
            t.getStatus().name(),
            t.getCreatedAt()
        );
    }
}