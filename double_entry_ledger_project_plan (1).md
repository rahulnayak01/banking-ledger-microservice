# Double-Entry Ledger Project — AI Agent Context

## Goal

Build a production-oriented **double-entry financial ledger system** using Java and Spring Boot.

The project should start as a well-structured Spring Boot service backed by PostgreSQL and progressively evolve into a distributed financial transaction system using concepts such as:

- Double-entry accounting
- Immutable ledger
- Transactional consistency
- Idempotency
- Concurrency control
- Balance projections
- Event sourcing / ledger replay
- Kafka-based asynchronous processing
- Redis
- Observability
- Fault tolerance
- Reconciliation

The primary goal is **learning + interview/system-design depth**, while keeping the implementation realistic enough to discuss as an industry-level project.

---

# Technology Stack

- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Maven
- Apache Kafka — introduced in a later phase
- Redis — introduced in a later phase
- Docker / Docker Compose

- No JUnit / Mockito test framework is required for this project

---

# Core Financial Principle

The ledger is the **source of truth**.

Every financial transaction must obey:

```text
Total Debits = Total Credits
```

Example:

```text
Transfer ₹100 from Account A to Account B

Account A → DEBIT  ₹100
Account B → CREDIT ₹100
```

A transaction must never leave the system in a partially applied state.

The ledger should be treated as **append-only/immutable** after posting.

Balances are derived/read-optimized data and should not be treated as the authoritative financial history.

---

# Target Architecture

Initial architecture:

```text
                 Spring Boot
                      |
        +-------------+-------------+
        |                           |
   Account APIs              Ledger/Transaction APIs
        |                           |
        +-------------+-------------+
                      |
                  PostgreSQL
                      |
          +-----------+-----------+
          |                       |
      Accounts               Ledger
                              |
                    +---------+---------+
                    |                   |
              Transactions        Ledger Entries
```

Later architecture:

```text
                 API Gateway
                      |
              Transaction Service
                      |
          +-----------+-----------+
          |                       |
      PostgreSQL                Redis
          |
       Ledger
          |
        Kafka
          |
   +------+------+----------------+
   |             |                |
Balance      Notification     Audit/Event
Projection      Service         Consumers
```

---

# End Requirements

By the end of the project, the system should support:

## 1. Account Management

- Create customer/account
- Account status
- Account type
- Currency
- Account ownership
- Account lookup

## 2. Double-Entry Transactions

Support:

- Deposit
- Withdrawal
- Account-to-account transfer

Every operation must create balanced ledger entries.

Example:

```text
Transaction ID: TXN-123

DEBIT  Account-A  ₹500
CREDIT Account-B  ₹500
```

## 3. Ledger

The ledger must:

- Be append-only after posting
- Store every financial movement
- Link entries to a transaction
- Store debit/credit direction
- Store exact monetary amount
- Store timestamps
- Support querying account history
- Support transaction history
- Allow rebuilding balances from ledger entries

## 4. Transactional Consistency

A financial transaction must be atomic.

If any part fails:

```text
Transaction
Debit Entry
Credit Entry
```

all changes must roll back.

Use PostgreSQL transactions / Spring:

```java
@Transactional
```

but do not rely only on the annotation; understand database isolation and locking behavior.

## 5. Idempotency

Requests must support an idempotency key.

Example:

```text
POST /transactions
Idempotency-Key: abc-123
```

If the same request is submitted multiple times, it must not create duplicate financial transactions.

Expected behavior:

```text
Request 1 → transaction created
Request 2 → same transaction returned
Request 3 → same transaction returned
```

## 6. Concurrency

Handle concurrent transactions against the same account.

Example:

```text
Balance = ₹1000

Request A → withdraw ₹800
Request B → withdraw ₹500
```

The system must prevent the account from incorrectly becoming negative because both requests read the same stale balance.

Study and implement appropriate PostgreSQL/JPA locking and transaction isolation.

## 7. Balance Projection

Maintain a fast balance representation for reads.

Conceptually:

```text
Ledger
   ↓
Balance Projection
   ↓
GET /accounts/{id}/balance
```

The balance is a projection, not the source of truth.

The system should be able to rebuild the balance from the ledger.

## 8. Ledger Replay

Implement a mechanism to:

```text
Delete/rebuild projection
        ↓
Read ledger
        ↓
Recalculate balances
        ↓
Recreate account balances
```

This demonstrates why an immutable ledger is useful.

## 9. Kafka

Introduce Kafka after the core transactional system works.

Use Kafka for asynchronous events such as:

```text
TransactionCompleted
BalanceUpdated
NotificationRequested
AuditEvent
```

Important concepts to demonstrate:

- Topics
- Partitions
- Consumer groups
- Producer acknowledgements
- Retry
- Dead-letter topic
- Ordering
- At-least-once delivery
- Duplicate event handling
- Consumer idempotency

Do NOT make Kafka responsible for the initial source-of-truth transaction unless there is a clear reason.

## 10. Outbox Pattern

Implement the transactional outbox pattern.

Example:

```text
PostgreSQL Transaction

Ledger Transaction
       +
Ledger Entries
       +
Outbox Event
       ↓
COMMIT
```

Then:

```text
Outbox Publisher
       ↓
Kafka
       ↓
Consumers
```

The objective is to avoid:

```text
DB commit succeeds
Kafka publish fails
```

and therefore losing an event.

## 11. Redis

Use Redis for appropriate non-authoritative use cases such as:

- Idempotency lookup/cache
- Rate limiting
- Short-lived transaction/request state where appropriate

Do NOT make Redis the financial source of truth.

## 12. Fault Tolerance

Eventually demonstrate:

- Retry
- Exponential backoff
- Kafka consumer retry
- Dead-letter topic
- Timeout
- Resilience patterns
- Idempotent consumers

## 13. Reconciliation

Build a reconciliation process that verifies:

```text
Sum of ledger entries
        vs
Account balance projection
```

Detect inconsistencies such as:

```text
Ledger says ₹1000
Projection says ₹900
```

The system should report the discrepancy rather than silently correcting financial history.

## 14. Observability

Add:

- Structured logging
- Correlation/request ID
- Micrometer metrics
- Prometheus
- Grafana
- Transaction latency
- DB connection pool metrics
- Kafka producer/consumer metrics
- Error counters
- Retry counters

---

# Development Phases

## Phase 1 — Project Foundation

### Goal

Create a clean Spring Boot project.

### Tasks

- Spring Boot setup
- Java 21
- Maven
- PostgreSQL
- Docker Compose
- Flyway
- JPA/Hibernate
- Configuration profiles
- Basic exception handling
- Project/package structure

### Expected Result

Application starts successfully and connects to PostgreSQL.

---

# Phase 2 — Customer and Account

### Goal

Create the basic domain model.

### Entities

```text
Customer
Account
```

### Account fields

At minimum:

```text
id
customer_id
account_number
account_type
currency
status
created_at
```

### APIs

```text
POST /customers
POST /accounts
GET /accounts/{id}
GET /accounts/{id}/balance
```

### Important Concepts

- Entity relationships
- UUIDs
- Validation
- Database constraints
- Flyway migrations
- Unique indexes

---

# Phase 3 — Double-Entry Ledger

### Goal

Build the actual accounting engine.

### Entities

```text
LedgerTransaction
LedgerEntry
```

### Enums

```java
EntryType {
    DEBIT,
    CREDIT
}
```

```java
TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
```

```java
TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
```

### LedgerTransaction

Conceptually:

```text
id
type
status
amount
created_at
```

### LedgerEntry

Conceptually:

```text
id
transaction_id
account_id
entry_type
amount
created_at
```

Relationship:

```text
LedgerTransaction 1 ---- * LedgerEntry
```

### First operation

Implement:

```text
DEBIT Account A ₹100
CREDIT Account B ₹100
```

### Invariants

- Amount must be > 0
- Every posted transaction must have balanced entries
- Total debit must equal total credit
- Transaction must be atomic
- Ledger entries cannot be modified after posting

---

# Phase 4 — Deposit / Withdrawal / Transfer

Implement:

```text
POST /transactions/deposit
POST /transactions/withdraw
POST /transactions/transfer
```

Introduce a system account for external money.

Example:

```text
Deposit ₹100

BANK-CASH       DEBIT  ₹100
CUSTOMER-A      CREDIT ₹100
```

Withdrawal:

```text
CUSTOMER-A      DEBIT  ₹100
BANK-CASH       CREDIT ₹100
```

Transfer:

```text
CUSTOMER-A      DEBIT  ₹100
CUSTOMER-B      CREDIT ₹100
```

Understand carefully why the system account is necessary for double-entry accounting.

---

# Phase 5 — Balance and Concurrency

### Goal

Make the financial operations safe under concurrent requests.

Implement:

- Balance projection
- Atomic balance updates
- Pessimistic locking or an appropriate optimistic strategy
- PostgreSQL transaction isolation
- Insufficient-funds validation
- Concurrent withdrawal tests

Example test:

```text
Initial balance = ₹1000

Thread 1 → withdraw ₹800
Thread 2 → withdraw ₹500
```

Only one transaction should be allowed if overdraft is not supported.

---

# Phase 6 — Idempotency

Implement:

```text
Idempotency-Key
```

Requirements:

- Unique key constraint
- Same request should not create another transaction
- Same key should return the original result
- Handle concurrent duplicate requests
- Define behavior when the same key is reused with different request payloads

Example:

```text
abc-123 + request A → TXN-1
abc-123 + request A → TXN-1
abc-123 + request B → reject
```

---

# Phase 7 — Ledger Replay

### Goal

Prove that the ledger is the source of truth.

Implement:

```text
rebuildBalances()
```

Process:

```text
Read ledger
   ↓
Group entries by account
   ↓
Calculate debit/credit effect
   ↓
Rebuild balances
```

Example:

```text
Account A

CREDIT ₹1000
DEBIT  ₹300
DEBIT  ₹200

Balance = ₹500
```

---

# Phase 8 — Transactional Outbox

Add:

```text
outbox_events
```

Within the same DB transaction:

```text
Ledger transaction
Ledger entries
Outbox event
```

must commit together.

Then create an outbox publisher that publishes events to Kafka.

---

# Phase 9 — Kafka

Introduce:

```text
transaction-events
balance-events
notification-events
```

Implement:

- Producer
- Consumer
- Consumer group
- Partitions
- Retry
- DLQ
- Consumer idempotency

Example:

```text
Transaction Service
        |
     PostgreSQL
        |
   Outbox Publisher
        |
       Kafka
        |
  +-----+--------+
  |              |
Balance       Notification
Consumer       Consumer
```

---

# Phase 10 — Redis

Introduce Redis for:

- Rate limiting
- Idempotency caching where useful
- Temporary data

Example rate limit:

```text
5 transaction requests / minute
```

Redis must never become the authoritative financial database.

---

# Phase 11 — Reconciliation

Create a scheduled reconciliation job.

Verify:

```text
Ledger-derived balance
        ==
Stored balance projection
```

Generate discrepancy reports.

Also test failure scenarios intentionally.

---

# Phase 12 — Production Hardening

Cover:

- Database indexes
- Connection pooling / HikariCP
- Query optimization
- Pagination
- API validation
- Security
- Authentication/authorization
- Rate limiting
- Graceful shutdown
- Docker
- Health checks
- Configuration management
- API/manual verification
- Load testing where useful

---

# Final Architecture

The final conceptual architecture should look approximately like:

```text
                         Clients
                            |
                       API Gateway
                            |
                  Transaction Service
                            |
             +--------------+--------------+
             |                             |
         PostgreSQL                       Redis
             |
      +------+----------------+
      |                       |
   Accounts                 Ledger
                              |
                     +--------+--------+
                     |                 |
              Transactions       Ledger Entries
                     |
                 Outbox
                     |
                     v
                   Kafka
                     |
          +----------+----------+
          |          |          |
      Balance   Notification   Audit
      Consumer    Consumer     Consumer
          |
          v
   Balance Projection

                 Prometheus
                     |
                   Grafana
```

---

# Non-Negotiable Design Rules

1. **Ledger is the source of truth.**
2. **Never use floating-point types for money.**
3. **Every posted transaction must balance.**
4. **Financial writes must be atomic.**
5. **Ledger entries are immutable after posting.**
6. **Balances are derived/projection data.**
7. **Duplicate requests must be safely handled.**
8. **Concurrent transactions must not corrupt balances.**
9. **Kafka events must be reliably produced; use the outbox pattern.**
10. **Consumers must tolerate duplicate messages.**
11. **Redis must not be the financial source of truth.**
12. **Reconciliation must detect projection/ledger mismatches.**
13. Every phase should include practical API/database verification before moving to the next phase.
14. Prefer database constraints and transactional guarantees over application-only assumptions.

---

# How the AI Agent Should Work

Work **one phase at a time**.

For each phase:

1. Explain the goal.
2. Explain the architecture/design decision.
3. Explain the database changes.
4. Provide complete Java code.
5. Provide Flyway migration.
6. Explain important code line-by-line where useful.
7. Provide API examples.
8. Provide test cases.
9. Explain failure/concurrency scenarios.
10. Stop and wait for confirmation before moving to the next phase.

Do not jump ahead unless explicitly asked.

When introducing a distributed-system concept, first explain **why it is needed**, then implement it.

Prioritize correctness and understanding over generating large amounts of code.

---

# End State

At completion, the project should demonstrate that the developer understands:

- Double-entry accounting
- ACID transactions
- PostgreSQL locking/isolation
- Immutable ledgers
- Event sourcing concepts
- Materialized/read projections
- Idempotency
- Concurrency
- Transactional outbox
- Kafka partitioning and consumer groups
- At-least-once delivery
- Redis
- Reconciliation
- Fault tolerance
- Production-oriented operational design
- Database performance
- Production-oriented Spring Boot design

The project should be explainable in a Java/Spring Boot backend or system-design interview, including the reasoning behind each architectural decision.
