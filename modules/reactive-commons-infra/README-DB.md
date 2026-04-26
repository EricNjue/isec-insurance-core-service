# Reactive Commons Infrastructure - Database Scripts

This module uses R2DBC for reactive database access. While the project uses Liquibase for main database migrations (see `app-bootstrap/src/main/resources/db/changelog/modules/38-reactive-commons.yaml`), the following SQL scripts are provided for reference or manual execution if needed.

## Outbox Events Table

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) UNIQUE,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
```

## Example Payment Table (Reactive)

If you are using the `PaymentServiceExample`, you may need a compatible `payments` table. Note that the JPA-based modules might already have a `payments` table with a different structure.

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```
