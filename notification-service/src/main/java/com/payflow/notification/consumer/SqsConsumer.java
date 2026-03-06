package com.payflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.notification.model.TransferEvent;
import com.payflow.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Component
public class SqsConsumer {

    private final SqsClient          sqsClient;
    private final NotificationService notificationService;
    private final ObjectMapper        objectMapper;
    private final String              queueUrl;

    public SqsConsumer(SqsClient sqsClient,
                       NotificationService notificationService,
                       ObjectMapper objectMapper,
                       @Value("${aws.sqs.queue-url}") String queueUrl) {
        this.sqsClient           = sqsClient;
        this.notificationService = notificationService;
        this.objectMapper        = objectMapper;
        this.queueUrl            = queueUrl;
    }

    // Polling cada 5 segundos
    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)   // Long polling
                    .build();

            List<Message> messages = sqsClient.receiveMessage(request).messages();

            for (Message message : messages) {
                processMessage(message);
            }
        } catch (Exception e) {
            System.err.println("❌ Error polling SQS: " + e.getMessage());
        }
    }

    private void processMessage(Message message) {
        try {
            System.out.println("📨 Mensaje recibido: " + message.body());

            TransferEvent event = objectMapper.readValue(
                    message.body(), TransferEvent.class);

            notificationService.notifyTransferCompleted(event);

            // Eliminar mensaje procesado de la cola
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

            System.out.println("✅ Mensaje procesado y eliminado de SQS");

        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje: " + e.getMessage());
            // No eliminamos el mensaje → SQS lo reintentará
        }
    }
}