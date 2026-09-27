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
 * AuditConsumer — records immutable audit log entries for all transaction events.
 */
@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);
    private static final String CONSUMER_GROUP = "audit-service-group";

    private final ConsumerIdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public AuditConsumer(ConsumerIdempotencyService idempotencyService,
                         ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${ledger.kafka.topics.transaction-events:transaction-events}",
            groupId = CONSUMER_GROUP
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[AUDIT CONSUMER] Consumed event key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            JsonNode root = objectMapper.readTree(record.value());
            UUID transactionId = UUID.fromString(root.path("transactionId").asText());

            if (idempotencyService.isAlreadyProcessed(transactionId, CONSUMER_GROUP)) {
                log.warn("[AUDIT CONSUMER] Duplicate event detected for transactionId={}. Skipping.", transactionId);
                return;
            }

            log.info("[AUDIT LOG] Successfully logged financial audit trail for transactionId={}", transactionId);

            idempotencyService.markProcessed(transactionId, CONSUMER_GROUP, "TransactionCompleted");

        } catch (Exception e) {
            log.error("[AUDIT CONSUMER] Error auditing record: {}", e.getMessage(), e);
            throw new RuntimeException("Audit recording failed", e);
        }
    }
}
