package com.example.ledger.outbox;

import com.example.ledger.kafka.producer.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background worker that reads pending outbox events from PostgreSQL
 * and publishes them to Kafka.
 *
 * <p>The transactional outbox guarantees that the business transaction
 * and the outbox event are committed atomically. This publisher then
 * asynchronously transfers the committed outbox event to Kafka.
 *
 * <p>Delivery semantics are at-least-once:
 * <ul>
 *     <li>The event is first committed to the database.</li>
 *     <li>The publisher sends the event to Kafka.</li>
 *     <li>Only after Kafka acknowledges the message is the event marked PUBLISHED.</li>
 *     <li>If publishing fails, the event remains PENDING and will be retried.</li>
 * </ul>
 */
@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaProducerService kafkaProducerService;

    @Value("${ledger.kafka.topics.transaction-events:transaction-events}")
    private String transactionEventsTopic;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaProducerService kafkaProducerService) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Periodically checks the outbox table for events that
     * have not yet been published.
     */
    @Scheduled(
            fixedDelayString =
                    "${ledger.outbox.poll-interval-ms:3000}"
    )
    public void schedulePublish() {

        try {
            int published = publishPendingEvents();
            if (published > 0) {
                log.info(
                        "Outbox poll completed. Published {} events.",
                        published
                );
            }
        } catch (Exception e) {
            log.error(
                    "Unexpected error while processing outbox events",
                    e
            );
        }
    }

    /**
     * Finds pending events and attempts to publish them to Kafka.
     *
     * <p>Each successful event is marked as PUBLISHED in its own
     * short database transaction.
     *
     * @return number of successfully published events
     */
    public int publishPendingEvents() {

        List<OutboxEvent> pendingEvents =
                findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return 0;
        }
        log.info(
                "Found {} pending outbox events",
                pendingEvents.size()
        );
        int publishedCount = 0;

        for (OutboxEvent event : pendingEvents) {

            try {
                /*
                 * 1. Publish to Kafka.
                 *
                 * sendEvent() must wait for Kafka acknowledgement.
                 */
                dispatchEvent(event);

                /*
                 * 2. Only after Kafka acknowledges the message,
                 *    mark the outbox event as published.
                 */
                markAsPublished(event);

                publishedCount++;

                log.info(
                        "Successfully published outbox event " +
                                "[ID: {}, Type: {}, AggregateId: {}]",
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId()
                );
            } catch (Exception e) {
                /*
                 * Do NOT mark the event as PUBLISHED.
                 *
                 * It remains PENDING and will be retried during
                 * the next scheduler execution.
                 */
                log.error(
                        "Failed to publish outbox event " +
                                "[ID: {}]. It will be retried. Error: {}",
                        event.getId(),
                        e.getMessage(),
                        e
                );
            }
        }
        return publishedCount;
    }

    /**
     * Reads pending events.
     *
     * This transaction is only used for the database read.
     */
    @Transactional(readOnly = true)
    protected List<OutboxEvent> findPendingEvents() {

        return outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                );
    }

    /**
     * Publishes one event to Kafka.
     *
     * <p>The Kafka producer must wait for the broker acknowledgement
     * before returning successfully.
     */
    protected void dispatchEvent(OutboxEvent event) {
        log.info(
                "[OUTBOX KAFKA DISPATCHER] Publishing event " +
                        "'{}' (Aggregate: {}) to topic '{}'",
                event.getEventType(),
                event.getAggregateId(),
                transactionEventsTopic
        );

        kafkaProducerService.sendEvent(
                transactionEventsTopic,
                event.getAggregateId().toString(),
                event.getPayload()
        );
    }

    /**
     * Marks the event as PUBLISHED.
     *
     * <p>This is deliberately a separate short transaction and is
     * executed only after Kafka has acknowledged the event.
     */
    @Transactional
    protected void markAsPublished(OutboxEvent event) {
        event.markPublished();
        outboxEventRepository.save(event);
    }
}