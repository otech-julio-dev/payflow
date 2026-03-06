package com.payflow.notification.service;

import com.payflow.notification.model.TransferEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class NotificationService {

    private final SnsClient snsClient;
    private final String    topicArn;

    public NotificationService(SnsClient snsClient,
                               @Value("${aws.sns.topic-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.topicArn  = topicArn;
    }

    public void notifyTransferCompleted(TransferEvent event) {
        // Notificación al emisor
        String senderMessage = buildSenderMessage(event);
        publish("💸 Transferencia enviada - PayFlow", senderMessage);

        // Notificación al receptor
        String receiverMessage = buildReceiverMessage(event);
        publish("💰 Dinero recibido - PayFlow", receiverMessage);

        System.out.printf("✅ Notificaciones enviadas para transferencia #%d%n",
                event.transferId());
    }

    private void publish(String subject, String message) {
        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(message)
                .build());
    }

    private String buildSenderMessage(TransferEvent event) {
        return """
                Hola,

                Tu transferencia ha sido procesada exitosamente.

                ─────────────────────────────
                Monto enviado:   $%s MXN
                Cuenta origen:   %s
                Cuenta destino:  %s
                Descripción:     %s
                Fecha:           %s
                ID:              #%d
                ─────────────────────────────

                Gracias por usar PayFlow.
                """.formatted(
                event.amount(),
                event.senderAccountNumber(),
                event.receiverAccountNumber(),
                event.description() != null ? event.description() : "Sin descripción",
                event.completedAt(),
                event.transferId()
        );
    }

    private String buildReceiverMessage(TransferEvent event) {
        return """
                Hola,

                Has recibido una transferencia en tu cuenta PayFlow.

                ─────────────────────────────
                Monto recibido:  $%s MXN
                Cuenta origen:   %s
                Cuenta destino:  %s
                Descripción:     %s
                Fecha:           %s
                ID:              #%d
                ─────────────────────────────

                Gracias por usar PayFlow.
                """.formatted(
                event.amount(),
                event.senderAccountNumber(),
                event.receiverAccountNumber(),
                event.description() != null ? event.description() : "Sin descripción",
                event.completedAt(),
                event.transferId()
        );
    }
}