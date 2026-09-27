````md
# Banking Ledger Microservice

A Spring Boot-based banking ledger system designed around transactional consistency, event-driven architecture, and scalable read models.

The system supports account management, deposits, withdrawals, transfers, transaction history, balance management, reconciliation, and transaction search. PostgreSQL acts as the primary source of truth, while Kafka distributes transaction events to independent consumers and read models.

## Overview

The project separates transactional operations from asynchronous processing and read-heavy workloads.

```text
                         ┌──────────────────────┐
                         │       REST API       │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Ledger Service    │
                         └──────────┬───────────┘
                                    │
                         ┌──────────┴───────────┐
                         │                      │
                         ▼                      ▼
                  ┌──────────────┐       ┌──────────────┐
                  │  PostgreSQL  │       │ Outbox Table │
                  │ Source of    │       │    Events    │
                  │ Truth        │       └──────┬───────┘
                  └──────────────┘              │
                                                ▼
                                          ┌───────────┐
                                          │   Kafka   │
                                          └─────┬─────┘
                                                │
                       ┌────────────────────────┼───────────────────────┐
                       │                        │                       │
                       ▼                        ▼                       ▼
                Balance Projection        Audit / Events        Elasticsearch
                                                                    Read Model
                                                                        │
                                                                        ▼
                                                                  Search API
````

## Features

### Account & Transaction Management

* Account creation and management
* Deposits
* Withdrawals
* Account-to-account transfers
* Transaction tracking
* Ledger entries
* Account balance management

### Ledger & Reconciliation

The ledger maintains transaction history independently from the current balance.

The system supports rebuilding and reconciling balances from ledger entries, allowing derived balance data to be verified against the underlying transaction history.

This provides a way to detect and recover from inconsistencies in derived balance data.

### Transactional Outbox

The project uses the Transactional Outbox pattern to reliably publish transaction events.

Business changes and their corresponding events are persisted within the same PostgreSQL transaction. A separate publisher then publishes pending events to Kafka.

This avoids the failure scenario where a database transaction succeeds but the corresponding Kafka event is never published.

```text
Database Transaction
        │
        ├── Business Data
        │
        └── Outbox Event
                │
                ▼
          Outbox Publisher
                │
                ▼
              Kafka
```

### Kafka Event Processing

Kafka is used as the event backbone for asynchronous processing.

Transaction events are consumed by independent consumer groups for different purposes, including:

* Balance projection
* Audit processing
* Notifications
* Elasticsearch indexing

Each consumer group can process the same transaction event independently without affecting the processing state of another consumer.

### Idempotent Consumers

Kafka-based processing is designed to handle duplicate event delivery.

The project maintains processed-event information using the combination of:

```text
(event_id, consumer_group)
```

This allows each consumer group to independently determine whether an event has already been processed.

For example:

```text
transaction-events
        │
        ├── audit-service-group
        ├── balance-projection-group
        ├── notification-service-group
        └── elasticsearch-group
```

Duplicate delivery to a consumer does not result in duplicate processing.

Elasticsearch documents also use the transaction ID as their document ID, providing another layer of protection against duplicate documents.

### Caching

Caffeine is used as an in-process cache for frequently accessed account balance data.

This allows commonly requested balances to be served without repeatedly querying the database.

Cache invalidation is tied to balance changes so that stale balance data is removed when the underlying transaction is committed.

Redis is also used for application state that needs to exist outside the application JVM, including use cases such as token management and rate limiting.

### Elasticsearch Read Model

Elasticsearch is used as a separate read model for transaction search.

PostgreSQL remains the authoritative source for financial data, while Elasticsearch contains a searchable projection of transaction events.

```text
PostgreSQL
    │
    ▼
Outbox
    │
    ▼
Kafka
    │
    ▼
Elasticsearch Consumer
    │
    ▼
Transaction Search Index
```

The search model supports transaction discovery based on account information and can be extended with additional filters such as:

* Transaction type
* Transaction status
* Currency
* Amount ranges
* Date ranges
* Debit account
* Credit account

This separates search-oriented workloads from the core transactional database.

Because Elasticsearch is populated asynchronously, the search model is eventually consistent with PostgreSQL.

## Data Flow

A typical transfer follows this high-level flow:

```text
Client
  │
  ▼
Transfer API
  │
  ▼
PostgreSQL Transaction
  │
  ├── Update financial state
  ├── Create ledger entry
  └── Create outbox event
          │
          ▼
       Kafka
          │
          ├── Balance Projection
          ├── Audit Consumer
          ├── Notification Consumer
          └── Elasticsearch Consumer
                                      │
                                      ▼
                                Search Read Model
```

The transaction is committed to PostgreSQL before the asynchronous consumers process the resulting event.

## Kafka Topics

The project uses Kafka topics for different types of events, including:

```text
transaction-events
balance-events
notification-events
transaction-events-dlt
```

The transaction event is used by multiple independent consumers to build different projections and perform different downstream operations.

## Consistency Model

The system uses different consistency models for different responsibilities.

### Transactional

PostgreSQL is responsible for:

* Financial transactions
* Ledger entries
* Account state
* Authoritative balances

### Eventually Consistent

Kafka-driven projections are asynchronous, including:

* Search data
* Balance projections
* Notifications
* Other derived read models

This separation allows the transactional system to remain authoritative while derived systems can scale independently.

## Failure Handling

The architecture accounts for common distributed-system failure scenarios.

### Duplicate Kafka Event

A duplicate event is detected through consumer idempotency and is not processed again.

### Consumer Failure

If a consumer fails while processing an event, Kafka can redeliver the event. Idempotent processing prevents the redelivery from creating duplicate results.

### Elasticsearch Failure

If Elasticsearch is temporarily unavailable, the PostgreSQL transaction remains unaffected because Elasticsearch is a downstream read model rather than the source of truth.

The event can be processed again once the consumer is able to successfully index it.

### Balance Inconsistency

If a derived balance becomes inconsistent, the ledger can be replayed to calculate the expected balance and reconcile the stored value.

## Technology Stack

| Technology      | Purpose                           |
| --------------- | --------------------------------- |
| Java            | Application development           |
| Spring Boot     | Backend framework                 |
| Spring Data JPA | PostgreSQL persistence            |
| PostgreSQL      | Primary transactional database    |
| Apache Kafka    | Event streaming                   |
| Elasticsearch   | Transaction search/read model     |
| Caffeine        | In-process caching                |
| Redis           | External/shared application state |
| Flyway          | Database migrations               |
| Docker          | Local infrastructure              |
| Maven           | Build and dependency management   |

## API Examples

### Account Operations

```http
POST /accounts
POST /accounts/{accountId}/deposit
POST /accounts/{accountId}/withdraw
POST /transfers
```

### Transaction Search

```http
GET /transactions/search/account/{accountNumber}
```

Example:

```http
GET /transactions/search/account/ACC-200
```

The search returns transactions where the specified account appears as either the debit or credit account.

Date and other transaction filters can be applied to narrow the search results.

## Project Goals

The main goal of the project is to demonstrate backend engineering concepts commonly used in distributed financial systems:

* Transactional consistency
* Event-driven architecture
* Reliable event publishing
* Kafka consumer design
* Idempotent processing
* Read model projections
* Cache management
* Data reconciliation
* Search-oriented data modeling
* Separation of transactional and read workloads

## Running Locally

The project uses Docker for infrastructure components such as Kafka and Elasticsearch.

Start the infrastructure with:

```bash
docker compose up -d
```

Then start the Spring Boot application using Maven or the IDE.

Elasticsearch is available locally at:

```text
http://localhost:9200
```

Kafka is exposed locally on:

```text
localhost:9092
```

## Architecture Summary

```text
PostgreSQL    → Source of Truth
Kafka         → Event Distribution
Caffeine      → Fast Local Reads
Redis         → External Shared State
Elasticsearch → Search Read Model
```

The combination provides a transactional core for financial operations while allowing asynchronous processing, caching, reconciliation, and scalable transaction search without making the derived systems responsible for financial correctness.

```
```
