package com.payflow.transaction.dto.request;

import java.math.BigDecimal;

public record CreateTransactionRequest(
    Long   userId,
    Long   counterpartyUserId,
    String accountNumber,
    String counterpartyAccountNumber,
    String type,           // CREDIT, DEBIT, TOPUP
    BigDecimal amount,
    BigDecimal balanceAfter,
    String description,
    String referenceId
) {}