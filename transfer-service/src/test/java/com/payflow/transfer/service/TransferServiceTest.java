package com.payflow.transfer.service;

import com.payflow.transfer.client.SqsProducer;
import com.payflow.transfer.client.TransactionClient;
import com.payflow.transfer.client.WalletClient;
import com.payflow.transfer.dto.request.TransferRequest;
import com.payflow.transfer.dto.response.TransferResponse;
import com.payflow.transfer.entity.Transfer;
import com.payflow.transfer.exception.TransferException;
import com.payflow.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService — Unit Tests")
class TransferServiceTest {

    @Mock TransferRepository  transferRepository;
    @Mock WalletClient        walletClient;
    @Mock TransactionClient   transactionClient;
    @Mock SqsProducer         sqsProducer;

    @InjectMocks TransferService transferService;

    // ── Fixtures ──────────────────────────────────────────────
    private static final Long   SENDER_ID      = 2L;
    private static final Long   RECEIVER_ID    = 3L;
    private static final String SENDER_ACCOUNT = "PF-0000000002-8B5D";
    private static final String RECEIVER_ACCOUNT = "PF-0000000003-86E5";
    private static final String TOKEN          = "Bearer test-token";
    private static final BigDecimal AMOUNT     = new BigDecimal("200.00");

    private Map<String, Object> senderAccountMap;
    private Map<String, Object> receiverAccountMap;

    @BeforeEach
    void setUp() {
        senderAccountMap = new HashMap<>();
        senderAccountMap.put("accountNumber", SENDER_ACCOUNT);
        senderAccountMap.put("userId",        SENDER_ID);
        senderAccountMap.put("balance",       "1000.00");

        receiverAccountMap = new HashMap<>();
        receiverAccountMap.put("accountNumber", RECEIVER_ACCOUNT);
        receiverAccountMap.put("userId",        RECEIVER_ID);
        receiverAccountMap.put("balance",       "500.00");
    }

    // ── Helpers ───────────────────────────────────────────────
    private Transfer savedTransfer(Long id) {
        Transfer t = new Transfer();
        t.setSenderUserId(SENDER_ID);
        t.setReceiverUserId(RECEIVER_ID);
        t.setSenderAccountNumber(SENDER_ACCOUNT);
        t.setReceiverAccountNumber(RECEIVER_ACCOUNT);
        t.setAmount(AMOUNT);
        t.setDescription("Pago de prueba");
        t.complete();
        // Simula el ID asignado por JPA
        try {
            var field = Transfer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(t, id);
        } catch (Exception ignored) {}
        return t;
    }

    // ── TEST 1: Transferencia exitosa ─────────────────────────
    @Test
    @DisplayName("transfer() — debería completar la transferencia exitosamente")
    void transfer_success() {
        // Arrange
        TransferRequest req = new TransferRequest(
            RECEIVER_ACCOUNT, AMOUNT, "Pago de prueba");

        Map<String, Object> senderAfter = new HashMap<>(senderAccountMap);
        senderAfter.put("balance", "800.00");
        Map<String, Object> receiverAfter = new HashMap<>(receiverAccountMap);
        receiverAfter.put("balance", "700.00");

        when(walletClient.getAccountByUserId(SENDER_ID, TOKEN))
            .thenReturn(senderAccountMap)
            .thenReturn(senderAfter);
        when(walletClient.getAccountByNumber(RECEIVER_ACCOUNT, TOKEN))
            .thenReturn(receiverAccountMap)
            .thenReturn(receiverAfter);
        when(transferRepository.save(any(Transfer.class)))
            .thenAnswer(inv -> {
                Transfer t = inv.getArgument(0);
                try {
                    var f = Transfer.class.getDeclaredField("id");
                    f.setAccessible(true);
                    if (f.get(t) == null) f.set(t, 1L);
                } catch (Exception ignored) {}
                return t;
            });

        // Act
        TransferResponse result =
            transferService.transfer(SENDER_ID, req, TOKEN);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.senderUserId()).isEqualTo(SENDER_ID);
        assertThat(result.receiverUserId()).isEqualTo(RECEIVER_ID);
        assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(result.status()).isEqualTo("COMPLETED");

        verify(walletClient).debit(SENDER_ID, AMOUNT, TOKEN);
        verify(walletClient).credit(RECEIVER_ID, AMOUNT, TOKEN);
        verify(transactionClient).recordDebit(any(), any(), any(), any(),
                any(), any(), any(), any());
        verify(transactionClient).recordCredit(any(), any(), any(), any(),
                any(), any(), any(), any());
        verify(sqsProducer).publishTransferCompleted(any(), eq(SENDER_ID),
                eq(RECEIVER_ID), any(), any(), eq(AMOUNT), any());
    }

    // ── TEST 2: Auto-transferencia ────────────────────────────
    @Test
    @DisplayName("transfer() — debería lanzar excepción si sender == receiver")
    void transfer_selfTransfer_throwsException() {
        // Arrange
        TransferRequest req = new TransferRequest(
            SENDER_ACCOUNT, AMOUNT, "Auto-transferencia");

        when(walletClient.getAccountByUserId(SENDER_ID, TOKEN))
            .thenReturn(senderAccountMap);

        // Act & Assert
        assertThatThrownBy(() ->
            transferService.transfer(SENDER_ID, req, TOKEN))
            .isInstanceOf(TransferException.class)
            .hasMessageContaining("ti mismo");

        verify(walletClient, never()).debit(any(), any(), any());
        verify(walletClient, never()).credit(any(), any(), any());
        verify(transferRepository, never()).save(any());
    }

    // ── TEST 3: Fallo en débito → transfer queda FAILED ───────
    @Test
    @DisplayName("transfer() — debería marcar FAILED si el débito falla")
    void transfer_debitFails_marksTransferFailed() {
        // Arrange
        TransferRequest req = new TransferRequest(
            RECEIVER_ACCOUNT, AMOUNT, "Test fallo");

        when(walletClient.getAccountByUserId(SENDER_ID, TOKEN))
            .thenReturn(senderAccountMap);
        when(walletClient.getAccountByNumber(RECEIVER_ACCOUNT, TOKEN))
            .thenReturn(receiverAccountMap);
        when(transferRepository.save(any(Transfer.class)))
            .thenAnswer(inv -> {
                Transfer t = inv.getArgument(0);
                try {
                    var f = Transfer.class.getDeclaredField("id");
                    f.setAccessible(true);
                    if (f.get(t) == null) f.set(t, 2L);
                } catch (Exception ignored) {}
                return t;
            });
        doThrow(new RuntimeException("Saldo insuficiente"))
            .when(walletClient).debit(SENDER_ID, AMOUNT, TOKEN);

        // Act & Assert
        assertThatThrownBy(() ->
            transferService.transfer(SENDER_ID, req, TOKEN))
            .isInstanceOf(TransferException.class)
            .hasMessageContaining("Error al procesar");

        // Verificar que se guardó con estado FAILED
        verify(transferRepository, atLeast(2)).save(argThat(t ->
            t.getStatus() != null &&
            t.getStatus().name().equals("FAILED")
        ));
        verify(walletClient, never()).credit(any(), any(), any());
        verify(sqsProducer, never()).publishTransferCompleted(
                any(), any(), any(), any(), any(), any(), any());
    }

    // ── TEST 4: getById — acceso autorizado ───────────────────
    @Test
    @DisplayName("getById() — debería retornar transferencia si usuario es sender")
    void getById_asSender_returnsTransfer() {
        // Arrange
        Transfer t = savedTransfer(10L);
        when(transferRepository.findById(10L)).thenReturn(Optional.of(t));

        // Act
        TransferResponse result = transferService.getById(10L, SENDER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.senderUserId()).isEqualTo(SENDER_ID);
    }

    // ── TEST 5: getById — acceso como receiver ────────────────
    @Test
    @DisplayName("getById() — debería retornar transferencia si usuario es receiver")
    void getById_asReceiver_returnsTransfer() {
        Transfer t = savedTransfer(10L);
        when(transferRepository.findById(10L)).thenReturn(Optional.of(t));

        TransferResponse result = transferService.getById(10L, RECEIVER_ID);

        assertThat(result.receiverUserId()).isEqualTo(RECEIVER_ID);
    }

    // ── TEST 6: getById — acceso no autorizado ────────────────
    @Test
    @DisplayName("getById() — debería lanzar excepción si usuario no tiene acceso")
    void getById_unauthorized_throwsException() {
        Transfer t = savedTransfer(10L);
        when(transferRepository.findById(10L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> transferService.getById(10L, 99L))
            .isInstanceOf(TransferException.class)
            .hasMessageContaining("acceso");
    }

    // ── TEST 7: getById — no encontrada ───────────────────────
    @Test
    @DisplayName("getById() — debería lanzar excepción si no existe")
    void getById_notFound_throwsException() {
        when(transferRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.getById(999L, SENDER_ID))
            .isInstanceOf(TransferException.class)
            .hasMessageContaining("no encontrada");
    }

    // ── TEST 8: getMyTransfers ────────────────────────────────
    @Test
    @DisplayName("getMyTransfers() — debería retornar lista de transferencias del usuario")
    void getMyTransfers_returnsUserTransfers() {
        Transfer t1 = savedTransfer(1L);
        Transfer t2 = savedTransfer(2L);
        when(transferRepository.findAllByUserId(SENDER_ID))
            .thenReturn(List.of(t1, t2));

        List<TransferResponse> result =
            transferService.getMyTransfers(SENDER_ID);

        assertThat(result).hasSize(2);
        verify(transferRepository).findAllByUserId(SENDER_ID);
    }

    // ── TEST 9: getMyTransfers — lista vacía ──────────────────
    @Test
    @DisplayName("getMyTransfers() — debería retornar lista vacía si no hay transferencias")
    void getMyTransfers_empty_returnsEmptyList() {
        when(transferRepository.findAllByUserId(99L))
            .thenReturn(List.of());

        List<TransferResponse> result = transferService.getMyTransfers(99L);

        assertThat(result).isEmpty();
    }
}
