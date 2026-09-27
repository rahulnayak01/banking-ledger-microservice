package com.example.ledger.kafka.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);

    Optional<ProcessedEvent> findByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
