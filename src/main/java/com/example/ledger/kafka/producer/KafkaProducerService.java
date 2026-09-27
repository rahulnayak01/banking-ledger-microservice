package com.example.ledger.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Handles publishing domain events to Kafka topics.
 */
@Service
public class KafkaProducerService {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Asynchronous Kafka publishing.
     *
     * Use this when the caller does not need to wait
     * for Kafka acknowledgement.
     */
    public CompletableFuture<SendResult<String, String>> sendEvent(
            String topic,
            String key,
            String payload) {

        log.info(
                "Publishing event to topic '{}' with key '{}'",
                topic,
                key
        );

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {

            if (ex == null) {

                log.info(
                        "Successfully sent event to topic '{}' " +
                                "[Partition: {}, Offset: {}] for key '{}'",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key
                );

            } else {

                log.error(
                        "Failed to publish event to topic '{}' " +
                                "for key '{}': {}",
                        topic,
                        key,
                        ex.getMessage(),
                        ex
                );
            }
        });

        return future;
    }

    /**
     * Synchronous Kafka publishing.
     *
     * Used by the OutboxPublisher.
     *
     * The method returns only after Kafka acknowledges
     * the message successfully.
     *
     * If Kafka publishing fails, an exception is thrown.
     */
    public SendResult<String, String> sendEventAndWait(
            String topic,
            String key,
            String payload) {

        log.info(
                "[OUTBOX] Publishing event to topic '{}' with key '{}'",
                topic,
                key
        );

        try {

            SendResult<String, String> result =
                    kafkaTemplate
                            .send(topic, key, payload)
                            .get();

            log.info(
                    "[OUTBOX] Kafka acknowledged event. " +
                            "topic='{}', partition={}, offset={}, key='{}'",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    key
            );

            return result;

        } catch (Exception e) {

            log.error(
                    "[OUTBOX] Failed to publish event to Kafka. " +
                            "topic='{}', key='{}'",
                    topic,
                    key,
                    e
            );

            throw new RuntimeException(
                    "Failed to publish event to Kafka",
                    e
            );
        }
    }
}