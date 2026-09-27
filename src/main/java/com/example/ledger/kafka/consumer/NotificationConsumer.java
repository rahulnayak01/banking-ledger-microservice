package com.example.ledger.kafka.consumer;

import com.example.ledger.kafka.idempotency.ConsumerIdempotencyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * NotificationConsumer — listens to transaction events and triggers notifications.
 * Demonstrates Consumer Idempotency to prevent sending duplicate notifications.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String CONSUMER_GROUP = "notification-service-group";

    private final ConsumerIdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(ConsumerIdempotencyService idempotencyService,
                                ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${ledger.kafka.topics.transaction-events:transaction-events}",
            groupId = CONSUMER_GROUP
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[NOTIFICATION CONSUMER] Received message from partition {} offset {}",
                record.partition(), record.offset());

        try {
            JsonNode root = objectMapper.readTree(record.value());
            UUID transactionId = UUID.fromString(root.path("transactionId").asText());

            // 1. Consumer Idempotency Check
            if (idempotencyService.isAlreadyProcessed(transactionId, CONSUMER_GROUP)) {
                log.warn("[NOTIFICATION CONSUMER] Duplicate event detected for transactionId={}. Skipping.", transactionId);
                return;
            }

            // 2. Process notification (e.g. SMS / Email / Push)
            String debitAccount = root.path("debitAccountNumber").asText();
            String creditAccount = root.path("creditAccountNumber").asText();
            double amount = root.path("amount").asDouble();
            String currency = root.path("currency").asText();

            log.info("[NOTIFICATION DISPATCH] Alert sent: Transferred {} {} from {} to {}",
                    amount, currency, debitAccount, creditAccount);

            // 3. Mark event as processed
            idempotencyService.markProcessed(transactionId, CONSUMER_GROUP, "TransactionCompleted");

        } catch (Exception e) {
            log.error("[NOTIFICATION CONSUMER] Error processing record: {}", e.getMessage(), e);
            throw new RuntimeException("Notification processing failed", e);
        }
    }
}
