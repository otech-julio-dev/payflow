package com.payflow.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class WalletClient {

    private final WebClient webClient;

    public WalletClient(@Value("${services.wallet-url}") String walletUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(walletUrl)
                .build();
    }

    // ── Obtener cuenta por userId ─────────────────────────────
    public Map<String, Object> getAccountByUserId(Long userId, String authToken) {
        return webClient.get()
                .uri("/api/wallets/me")
                .header("Authorization", "Bearer " + authToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    // ── Obtener cuenta por accountNumber ─────────────────────
    public Map<String, Object> getAccountByNumber(String accountNumber, String authToken) {
        return webClient.get()
                .uri("/api/wallets/by-account/{number}", accountNumber)
                .header("Authorization", "Bearer " + authToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    // ── Débito (salida de dinero) ─────────────────────────────
    public void debit(Long userId, BigDecimal amount, String authToken) {
        webClient.post()
                .uri("/api/wallets/internal/debit")
                .header("Authorization", "Bearer " + authToken)
                .bodyValue(Map.of("userId", userId, "amount", amount))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // ── Crédito (entrada de dinero) ───────────────────────────
    public void credit(Long userId, BigDecimal amount, String authToken) {
        webClient.post()
                .uri("/api/wallets/internal/credit")
                .header("Authorization", "Bearer " + authToken)
                .bodyValue(Map.of("userId", userId, "amount", amount))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}