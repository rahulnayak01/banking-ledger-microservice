package com.example.ledger.kafka.consumer;

import com.example.ledger.elasticsearch.TransactionDocument;
import com.example.ledger.elasticsearch.TransactionSearchRepository;
import com.example.ledger.kafka.idempotency.ConsumerIdempotencyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TransactionIndexConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionIndexConsumer.class);

    private static final String CONSUMER_GROUP = "elasticsearch-group";

    private final ConsumerIdempotencyService idempotencyService;
    private final TransactionSearchRepository transactionSearchRepository;
    private final ObjectMapper objectMapper;

    public TransactionIndexConsumer(
            ConsumerIdempotencyService idempotencyService,
            TransactionSearchRepository transactionSearchRepository,
            ObjectMapper objectMapper
    ) {
        this.idempotencyService = idempotencyService;
        this.transactionSearchRepository = transactionSearchRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${ledger.kafka.topics.transaction-events:transaction-events}",
            groupId = CONSUMER_GROUP
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            UUID transactionId = UUID.fromString(root.path("transactionId").asText());

            if (idempotencyService.isAlreadyProcessed(transactionId, CONSUMER_GROUP)) {
                log.warn("Duplicate Kafka event. Skipping transactionId={}", transactionId);
                return;
            }

            String docId = transactionId.toString();
            if (transactionSearchRepository.existsById(docId)) {
                log.warn("Elasticsearch document already exists for transactionId={}. Skipping.", transactionId);
                idempotencyService.markProcessed(transactionId, CONSUMER_GROUP, "TransactionCompleted");
                return;
            }

            TransactionDocument doc = new TransactionDocument();
            doc.setId(docId);
            doc.setTransactionType(root.path("transactionType").asText());
            doc.setStatus(root.path("status").asText());
            doc.setCurrency(root.path("currency").asText());
            doc.setAmount(new BigDecimal(root.path("amount").asText()));
            doc.setDebitAccountNumber(root.path("debitAccountNumber").asText());
            doc.setCreditAccountNumber(root.path("creditAccountNumber").asText());
            doc.setCreatedAt(java.time.OffsetDateTime.parse(root.path("createdAt").asText()));

            transactionSearchRepository.save(doc);

            idempotencyService.markProcessed(transactionId, CONSUMER_GROUP, "TransactionCompleted");

            log.info("Indexed transaction {} into Elasticsearch", transactionId);

        } catch (Exception e) {
            log.error("Elasticsearch indexing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Indexing failed", e);
        }
    }
}