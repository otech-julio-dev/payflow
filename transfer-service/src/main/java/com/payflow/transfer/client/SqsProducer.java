package com.payflow.transfer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class SqsProducer {

    private final SqsClient    sqsClient;
    private final ObjectMapper objectMapper;
    private final String       queueUrl;

    public SqsProducer(@Value("${aws.sqs.queue-url}") String queueUrl,
                       ObjectMapper objectMapper) {
        this.sqsClient = SqsClient.builder()
                .region(Region.of("us-east-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = objectMapper;
        this.queueUrl     = queueUrl;
    }

    public void publishTransferCompleted(Long transferId,
                                         Long senderUserId,
                                         Long receiverUserId,
                                         String senderAccountNumber,
                                         String receiverAccountNumber,
                                         BigDecimal amount,
                                         String description) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("transferId",            transferId);
            event.put("senderUserId",          senderUserId);
            event.put("receiverUserId",        receiverUserId);
            event.put("senderEmail",           "sender@payflow.com");
            event.put("receiverEmail",         "receiver@payflow.com");
            event.put("senderAccountNumber",   senderAccountNumber);
            event.put("receiverAccountNumber", receiverAccountNumber);
            event.put("amount",                amount);
            event.put("description",           description != null ? description : "");
            event.put("status",                "COMPLETED");
            event.put("completedAt",           LocalDateTime.now().toString());

            String body = objectMapper.writeValueAsString(event);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());

            System.out.println("📤 Evento publicado en SQS: transferId=" + transferId);

        } catch (Exception e) {
            System.err.println("⚠️ Warning: No se pudo publicar en SQS: "
                + e.getMessage());
        }
    }
}