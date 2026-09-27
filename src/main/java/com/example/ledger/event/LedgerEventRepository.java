package com.example.ledger.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEventRepository
        extends JpaRepository<LedgerEvent, UUID> {

    List<LedgerEvent>
    findByAggregateIdOrderBySequenceNumberAsc(UUID aggregateId);
}