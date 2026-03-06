package com.payflow.notification.model;

import java.math.BigDecimal;

public record TransferEvent(
    Long       transferId,
    Long       senderUserId,
    Long       receiverUserId,
    String     senderEmail,
    String     receiverEmail,
    String     senderAccountNumber,
    String     receiverAccountNumber,
    BigDecimal amount,
    String     description,
    String     status,
    String     completedAt
) {}