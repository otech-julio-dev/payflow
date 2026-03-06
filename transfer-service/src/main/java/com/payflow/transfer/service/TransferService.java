package com.payflow.transfer.service;

import com.payflow.transfer.client.WalletClient;
import com.payflow.transfer.dto.request.TransferRequest;
import com.payflow.transfer.dto.response.TransferResponse;
import com.payflow.transfer.entity.Transfer;
import com.payflow.transfer.exception.TransferException;
import com.payflow.transfer.repository.TransferRepository;
import com.payflow.transfer.client.TransactionClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final WalletClient       walletClient;
    private final TransactionClient   transactionClient;

    public TransferService(TransferRepository transferRepository,
                           WalletClient walletClient,
                       TransactionClient transactionClient) {
        this.transferRepository = transferRepository;
        this.walletClient       = walletClient;
        this.transactionClient   = transactionClient;
    }

    @Transactional
    public TransferResponse transfer(Long senderUserId,
                                     TransferRequest req,
                                     String authToken) {
        // 1. Obtener cuenta del emisor
        Map<String, Object> senderAccount =
            walletClient.getAccountByUserId(senderUserId, authToken);

        String senderAccountNumber =
            (String) senderAccount.get("accountNumber");

        // 2. Validar que no se transfiere a sí mismo
        if (senderAccountNumber.equals(req.targetAccountNumber())) {
            throw new TransferException("No puedes transferirte a ti mismo");
        }

        // 3. Obtener cuenta del receptor
        Map<String, Object> receiverAccount =
            walletClient.getAccountByNumber(req.targetAccountNumber(), authToken);

        Long receiverUserId =
            ((Number) receiverAccount.get("userId")).longValue();

        // 4. Crear registro de transferencia en PENDING
        Transfer transfer = new Transfer();
        transfer.setSenderUserId(senderUserId);
        transfer.setReceiverUserId(receiverUserId);
        transfer.setSenderAccountNumber(senderAccountNumber);
        transfer.setReceiverAccountNumber(req.targetAccountNumber());
        transfer.setAmount(req.amount());
        transfer.setDescription(req.description());
        transferRepository.save(transfer);

        // 5. Ejecutar débito y crédito
        try {
        // Débito en cuenta origen
        walletClient.debit(senderUserId, req.amount(), authToken);
        Map<String, Object> senderAfter =
            walletClient.getAccountByUserId(senderUserId, authToken);
        BigDecimal senderBalanceAfter =
            new BigDecimal(senderAfter.get("balance").toString());

        // Crédito en cuenta destino
        walletClient.credit(receiverUserId, req.amount(), authToken);
        Map<String, Object> receiverAfter =
            walletClient.getAccountByNumber(req.targetAccountNumber(), authToken);
        BigDecimal receiverBalanceAfter =
            new BigDecimal(receiverAfter.get("balance").toString());

        transfer.complete();
        transferRepository.save(transfer);

        // Registrar en MongoDB (async — no bloquea si falla)
        String refId = "TRF-" + transfer.getId();
        transactionClient.recordDebit(
            senderUserId, receiverUserId,
            senderAccountNumber, req.targetAccountNumber(),
            req.amount(), senderBalanceAfter,
            req.description(), refId
        );
        transactionClient.recordCredit(
            receiverUserId, senderUserId,
            req.targetAccountNumber(), senderAccountNumber,
            req.amount(), receiverBalanceAfter,
            req.description(), refId
        );

    } catch (Exception e) {
        transfer.fail(e.getMessage());
        transferRepository.save(transfer);
        throw new TransferException("Error al procesar la transferencia: "
            + e.getMessage());
    }

        transferRepository.save(transfer);
        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferResponse getById(Long id, Long userId) {
        Transfer transfer = transferRepository.findById(id)
            .orElseThrow(() -> new TransferException("Transferencia no encontrada"));

        if (!transfer.getSenderUserId().equals(userId)
                && !transfer.getReceiverUserId().equals(userId)) {
            throw new TransferException("No tienes acceso a esta transferencia");
        }
        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getMyTransfers(Long userId) {
        return transferRepository.findAllByUserId(userId)
                .stream()
                .map(TransferResponse::from)
                .toList();
    }
}