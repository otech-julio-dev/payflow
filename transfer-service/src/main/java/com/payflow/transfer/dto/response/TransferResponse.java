package com.payflow.transfer.dto.response;

import com.payflow.transfer.entity.Transfer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
    Long           id,
    Long           senderUserId,
    Long           receiverUserId,
    String         senderAccountNumber,
    String         receiverAccountNumber,
    BigDecimal     amount,
    String         description,
    String         status,
    LocalDateTime  createdAt,
    LocalDateTime  completedAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
            t.getId(),
            t.getSenderUserId(),
            t.getReceiverUserId(),
            t.getSenderAccountNumber(),
            t.getReceiverAccountNumber(),
            t.getAmount(),
            t.getDescription(),
            t.getStatus().name(),
            t.getCreatedAt(),
            t.getCompletedAt()
        );
    }
}