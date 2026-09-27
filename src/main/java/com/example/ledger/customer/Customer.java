package com.example.ledger.customer;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_customer_id", nullable = false, unique = true)
    private String externalCustomerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Customer() {
    }

    public Customer(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getExternalCustomerId() {
        return externalCustomerId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}