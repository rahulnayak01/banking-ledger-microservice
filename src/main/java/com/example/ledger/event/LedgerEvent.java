package com.example.ledger.event;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_sequence",
                        columnNames = {
                                "aggregate_id",
                                "sequence_number"
                        }
                )
        }
)
public class LedgerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerEvent() {
    }

    public LedgerEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Long sequenceNumber,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getPayload() {
        return payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}