package com.example.ledger.kafka.idempotency;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks events successfully processed by each consumer group.
 * Ensures Idempotent Consumers under Kafka's At-Least-Once message delivery.
 */
@Entity
@Table(name = "processed_events",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "consumer_group"}))
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId, String consumerGroup, String eventType) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.eventType = eventType;
        this.processedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public String getEventType() {
        return eventType;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
