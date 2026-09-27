package com.example.ledger.outbox;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for inspecting outbox events and triggering publishing on demand.
 */
@RestController
@RequestMapping("/outbox")
public class OutboxController {

    private final OutboxService outboxService;
    private final OutboxPublisher outboxPublisher;

    public OutboxController(OutboxService outboxService,
                            OutboxPublisher outboxPublisher) {
        this.outboxService = outboxService;
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * GET /outbox/events
     * Lists all recorded outbox events.
     */
    @GetMapping("/events")
    public List<OutboxEvent> getAllEvents() {
        return outboxService.getAllEvents();
    }

    /**
     * GET /outbox/pending
     * Lists pending outbox events waiting to be published.
     */
    @GetMapping("/pending")
    public List<OutboxEvent> getPendingEvents() {
        return outboxService.getPendingEvents();
    }

    /**
     * GET /outbox/events/{aggregateId}
     * Lists outbox events for a given transaction or entity aggregate.
     */
    @GetMapping("/events/{aggregateId}")
    public List<OutboxEvent> getEventsForAggregate(@PathVariable UUID aggregateId) {
        return outboxService.getEventsForAggregate(aggregateId);
    }

    /**
     * POST /outbox/publish
     * Manually triggers immediate processing and dispatch of all pending outbox events.
     */
//    @PostMapping("/publish")
//    @ResponseStatus(HttpStatus.OK)
//    public Map<String, Object> publishPending() {
//        int published = outboxPublisher.publishPendingEvents();
//        return Map.of(
//                "status", "SUCCESS",
//                "eventsPublished", published
//        );
//    }
}
