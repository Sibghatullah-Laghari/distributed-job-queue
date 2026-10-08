# Smart Job Queue — Engineering Instructions

## 1. Project Goal

Build a **production-inspired Smart Job Queue** in Java 21 + Spring Boot + PostgreSQL + RabbitMQ + Docker.

This is intentionally a learning project, but the architecture should demonstrate how a real distributed backend system is designed.

The priority is:

**System design > architecture > concurrency > reliability > correctness > clean/simple code > features.**

Do not over-engineer individual classes. Keep implementation simple while making the **system architecture strong, explicit, and defensible**.

The project should be easy to explain in a backend/system-design interview.

---

## 2. Core System

Basic flow:

Client
→ REST API
→ Job Service
→ PostgreSQL
→ Message Broker
→ Workers
→ Job State Update
→ Client/API

Users submit jobs.

Jobs have states:

`PENDING → RUNNING → COMPLETED`

Failure:

`RUNNING → RETRYING → RUNNING`

After retry exhaustion:

`RETRYING → FAILED → DLQ`

Track:
- job ID
- type
- payload
- status
- priority
- attempts
- timestamps
- worker information
- error information

---

## 3. Architecture Principles

Design this as a **small distributed system**, not a CRUD application.

Prioritize:

- loose coupling
- horizontal scalability
- fault isolation
- concurrency safety
- idempotency
- retry safety
- failure recovery
- backpressure
- observability
- predictable state transitions
- graceful degradation
- stateless API nodes
- independent workers

Assume:

- multiple API instances may run simultaneously
- multiple workers may process jobs simultaneously
- messages may be duplicated
- messages may arrive late
- workers may crash
- network calls may fail
- database transactions may fail
- broker may temporarily become unavailable
- requests may arrive concurrently

Never design around the assumption that only one server or worker exists.

---

## 4. Important System Design Concepts

Deliberately incorporate and demonstrate:

### Concurrency
- multiple workers
- concurrent job submission
- safe state transitions
- atomic database operations
- optimistic/pessimistic locking where appropriate
- thread-safe components
- bounded worker concurrency

### Reliability
- acknowledgements
- retry
- exponential backoff
- dead-letter queue
- failure recovery
- graceful shutdown
- visibility/recovery strategy for stuck jobs

### Delivery Semantics

Target:

**at-least-once delivery + idempotent processing**

Do NOT pretend RabbitMQ/database integration magically provides exactly-once processing.

Design for duplicate delivery.

### Idempotency

Support an idempotency key/job request ID.

Repeated submission of the same logical request should not accidentally create duplicate work.

Workers must also safely handle duplicate messages.

### Backpressure

Do not allow unlimited work to enter worker memory.

Use:
- broker queue
- bounded concurrency
- controlled prefetch
- rejection/rate limiting where appropriate

### Scalability

API layer should be stateless.

Workers should scale horizontally:

`Worker 1`
`Worker 2`
`Worker 3`
`...`

All workers consume from the same logical queue.

Avoid designs that require a single worker or global in-memory state.

---

## 5. Data Consistency

PostgreSQL is the source of truth for job state.

Define valid state transitions explicitly.

Example:

`PENDING → RUNNING`
`RUNNING → COMPLETED`
`RUNNING → RETRYING`
`RETRYING → RUNNING`
`RETRYING → FAILED`

Invalid transitions must be rejected.

Avoid race conditions such as:

Worker A marks job RUNNING
while
Worker B also marks the same job RUNNING.

Use database constraints/atomic updates/locking where justified.

Do not solve distributed consistency problems using random `synchronized` blocks.

---

## 6. Transaction Boundaries

Think carefully about:

**Database transaction vs message publication vs message acknowledgement.**

Do not claim that a normal DB transaction automatically makes PostgreSQL and RabbitMQ atomic.

If needed, introduce an **Outbox Pattern** as an architectural evolution.

Preferred learning progression:

1. Basic DB + RabbitMQ
2. Identify dual-write problem
3. Implement Outbox Pattern
4. Publisher reads outbox
5. Publish message
6. Mark outbox event published
7. Design for duplicate publication

The goal is to understand the trade-off, not merely add patterns.

---

## 7. Job Processing

Worker flow should conceptually be:

Consume message
→ validate
→ acquire/verify job ownership/state
→ mark RUNNING
→ execute
→ mark COMPLETED

On failure:

execute
→ classify failure
→ retry if retryable
→ increment attempt
→ delayed retry/backoff
→ eventually FAILED
→ DLQ

Differentiate:

- transient failure
- permanent failure
- malformed job
- infrastructure failure

Do not retry everything blindly.

---

## 8. Worker Design

Workers must be independently scalable.

A worker should:

- consume jobs
- limit concurrency
- avoid blocking unnecessarily
- handle exceptions
- acknowledge only after appropriate processing
- update job state safely
- recover from crashes
- shut down gracefully

Consider:

- worker ID
- heartbeat
- processing timeout
- stuck-job detection
- graceful shutdown
- cancellation

Keep these mechanisms simple unless they materially improve the learning value.

---

## 9. API Design

Provide clean REST APIs.

Example:

`POST /api/jobs`
`GET /api/jobs/{id}`
`GET /api/jobs`
`POST /api/jobs/{id}/cancel`

Use:

- DTOs
- validation
- consistent error responses
- HTTP status codes
- pagination
- filtering
- request IDs/correlation IDs

Do not expose JPA entities directly.

---

## 10. Database

Use PostgreSQL.

Important entities:

### Job
- id
- idempotency_key
- type
- payload
- status
- priority
- attempt_count
- max_attempts
- created_at
- updated_at
- started_at
- completed_at
- worker_id
- last_error

### OutboxEvent
- id
- aggregate/job ID
- event type
- payload
- created_at
- published_at
- retry_count

Add appropriate indexes.

Think about query patterns before adding indexes.

Avoid unnecessary normalization/complex schemas.

---

## 11. RabbitMQ

Use RabbitMQ for asynchronous execution.

Demonstrate:

- exchange
- queue
- routing key
- acknowledgement
- prefetch
- retry strategy
- DLQ
- dead-letter exchange
- competing consumers

Do not create dozens of queues/exchanges without architectural justification.

---

## 12. Failure Scenarios

The design should explicitly consider:

1. API crashes after DB commit.
2. API crashes before DB commit.
3. API creates DB record but message publication fails.
4. Message is delivered twice.
5. Worker crashes during processing.
6. Worker crashes after completing work but before ACK.
7. Database temporarily unavailable.
8. RabbitMQ temporarily unavailable.
9. Job permanently fails.
10. Job exceeds retry limit.
11. Two workers race for the same job.
12. Worker becomes stuck.
13. Large number of jobs arrive suddenly.
14. API receives duplicate request.
15. Multiple API instances process requests concurrently.

For each, prefer a simple, explicit recovery strategy.

---

## 13. Observability

Include basic production-style observability:

- structured logging
- correlation/request ID
- job ID in logs
- worker ID in logs
- metrics
- processing duration
- queue depth
- success/failure count
- retry count
- active workers

Use Spring Boot Actuator where useful.

Do not build a full observability platform.

---

## 14. Performance

The system should be designed for concurrent workloads.

Focus on:

- bounded thread pools
- connection pool sizing
- RabbitMQ prefetch
- batch operations where useful
- avoiding unnecessary DB queries
- efficient indexes
- avoiding blocking operations
- controlled concurrency

Do not prematurely optimize.

Every optimization should have a reason.

---

## 15. Code Quality

Keep code:

- simple
- readable
- modular
- testable
- boring where possible

Follow:

- SOLID where useful
- separation of concerns
- dependency inversion
- DTO/entity separation
- service/repository boundaries
- configuration externalization
- meaningful naming

Avoid:

- unnecessary abstractions
- excessive interfaces
- generic "util" classes
- premature design patterns
- huge services
- god classes
- clever code
- over-engineering

**Prefer the simplest design that preserves the architectural property we need.**

---

## 16. Architecture Layers

Prefer:

`controller`
→ `application/service`
→ `domain`
→ `repository/infrastructure`

Keep infrastructure concerns isolated.

Suggested packages:

```text
job/
  controller/
  service/
  domain/
  repository/
  dto/

worker/
  consumer/
  service/

outbox/
  service/
  repository/
  publisher/

common/
  exception/
  config/
  observability/
```

Adjust package structure when the architecture benefits from it.

---

## 17. Testing

Do not only test controllers.

Prioritize:

### Unit tests
- state transitions
- retry policy
- idempotency
- failure classification

### Integration tests
- PostgreSQL
- RabbitMQ
- job lifecycle

### Concurrency tests
Test scenarios where multiple workers/processes attempt to process the same job.

Use Testcontainers where practical.

---

## 18. Development Strategy

Build incrementally.

### Phase 1
Basic job submission + persistence.

### Phase 2
RabbitMQ + worker.

### Phase 3
Job lifecycle/state machine.

### Phase 4
Retry + DLQ.

### Phase 5
Idempotency.

### Phase 6
Concurrency protection.

### Phase 7
Outbox pattern.

### Phase 8
Worker recovery/stuck jobs.

### Phase 9
Observability.

### Phase 10
Load/concurrency testing.

### Phase 11
Dockerized deployment.

Do not implement everything at once.

After each phase:
- compile
- test
- verify behavior
- review architecture
- then continue.

---

## 19. System Design Documentation

Maintain a concise `docs/architecture.md`.

Document:

- architecture diagram
- component responsibilities
- data flow
- job lifecycle
- state machine
- failure scenarios
- retry strategy
- idempotency strategy
- concurrency strategy
- scaling strategy
- Outbox decision
- consistency trade-offs
- bottlenecks
- future improvements

Documentation should explain **why**, not merely **what**.

---

## 20. Coding Agent Rules

Before changing code:

1. Understand the existing architecture.
2. Search for existing implementations.
3. Reuse existing abstractions when appropriate.
4. Identify affected components.
5. Consider concurrency/failure implications.
6. Make the smallest coherent change.

After changing code:

1. Compile.
2. Run relevant tests.
3. Fix failures.
4. Check for architectural regressions.
5. Summarize what changed and why.

Never rewrite large parts of the project without necessity.

Never introduce a library/pattern without explaining its architectural reason.

When uncertain between two designs, prefer the simpler design and document the trade-off.

---

## 21. Most Important Rule

This is **not primarily a CRUD project**.

The CRUD/API layer is only the entry point.

The real project is:

**How do we reliably execute large numbers of asynchronous jobs across multiple concurrent workers while handling failure, duplication, retries, backpressure, consistency, and horizontal scaling?**

Optimize decisions around that question.

The implementation should remain small enough for a student to understand completely, but the architecture should be strong enough to discuss in a backend/system-design interview.

**Build like a small infrastructure system, not like a demo CRUD application.**