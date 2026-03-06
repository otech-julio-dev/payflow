package com.payflow.wallet.dto.request;

import java.math.BigDecimal;

public record InternalOperationRequest(Long userId, BigDecimal amount) {}