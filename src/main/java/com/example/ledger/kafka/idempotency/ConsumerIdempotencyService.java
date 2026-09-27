package com.example.ledger.kafka.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ensures that consumers do not duplicate processing if Kafka delivers
 * an event more than once (At-Least-Once delivery semantics).
 */
@Service
public class ConsumerIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerIdempotencyService.class);

    private final ProcessedEventRepository processedEventRepository;

    public ConsumerIdempotencyService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Checks if this event was already processed by this consumer group.
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(UUID eventId, String consumerGroup) {
        return processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup);
    }

    /**
     * Records the event as processed for this consumer group.
     *
     * @return true if successfully marked (first time), false if duplicate caught by DB constraint
     */
    @Transactional
    public boolean markProcessed(UUID eventId, String consumerGroup, String eventType) {
        try {
            ProcessedEvent record = new ProcessedEvent(eventId, consumerGroup, eventType);
            processedEventRepository.save(record);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected by unique constraint for eventId={} and consumerGroup={}",
                    eventId, consumerGroup);
            return false;
        }
    }
}
