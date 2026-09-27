package com.example.ledger.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Saves an outbox event. Must be called within the same transactional context
     * as the financial ledger write (MANDATORY or default REQUIRED propagation).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveEvent(String aggregateType,
                                 UUID aggregateId,
                                 String eventType,
                                 String payload) {
        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, payload);
        return outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getAllEvents() {
        return outboxEventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsForAggregate(UUID aggregateId) {
        return outboxEventRepository.findByAggregateId(aggregateId);
    }
}
