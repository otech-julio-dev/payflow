package com.payflow.wallet.dto.response;

import com.payflow.wallet.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
    Long          id,
    Long          userId,
    String        accountNumber,
    BigDecimal    balance,
    String        status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getUserId(),
            account.getAccountNumber(),
            account.getBalance(),
            account.getStatus().name(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}