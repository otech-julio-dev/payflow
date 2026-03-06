package com.payflow.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class TransactionClient {

    private final WebClient webClient;

    public TransactionClient(
            @Value("${services.transaction-url}") String transactionUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(transactionUrl)
                .build();
    }

    public void recordDebit(Long userId, Long toUserId,
                            String accountNumber, String toAccountNumber,
                            BigDecimal amount, BigDecimal balanceAfter,
                            String description, String referenceId) {
        record(userId, toUserId, accountNumber, toAccountNumber,
               "DEBIT", amount, balanceAfter, description, referenceId);
    }

    public void recordCredit(Long userId, Long fromUserId,
                             String accountNumber, String fromAccountNumber,
                             BigDecimal amount, BigDecimal balanceAfter,
                             String description, String referenceId) {
        record(userId, fromUserId, accountNumber, fromAccountNumber,
               "CREDIT", amount, balanceAfter, description, referenceId);
    }

    private void record(Long userId, Long counterpartyUserId,
                        String accountNumber, String counterpartyAccountNumber,
                        String type, BigDecimal amount, BigDecimal balanceAfter,
                        String description, String referenceId) {
        try {
            webClient.post()
                .uri("/api/transactions/internal")
                .bodyValue(Map.of(
                    "userId",                   userId,
                    "counterpartyUserId",        counterpartyUserId != null
                                                    ? counterpartyUserId : 0,
                    "accountNumber",             accountNumber,
                    "counterpartyAccountNumber", counterpartyAccountNumber != null
                                                    ? counterpartyAccountNumber : "",
                    "type",                      type,
                    "amount",                    amount,
                    "balanceAfter",              balanceAfter,
                    "description",               description != null ? description : "",
                    "referenceId",               referenceId
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (Exception e) {
            // Log pero no falla la transferencia si el registro falla
            System.err.println("⚠️ Warning: No se pudo registrar transacción: "
                + e.getMessage());
        }
    }
}