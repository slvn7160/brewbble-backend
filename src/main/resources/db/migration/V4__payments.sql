-- V4__payments.sql – Square payment integration

ALTER TABLE orders
    ADD COLUMN payment_status    VARCHAR(20)  NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN square_payment_id VARCHAR(100);          -- Square's payment ID for refunds/tracking

-- Full payment event log — audit trail and webhook idempotency
CREATE TABLE payment_events (
    id                BIGSERIAL    PRIMARY KEY,
    order_id          BIGINT                    REFERENCES orders(id) ON DELETE SET NULL,
    square_event_id   VARCHAR(100) NOT NULL UNIQUE,    -- prevents duplicate processing
    square_payment_id VARCHAR(100),
    event_type        VARCHAR(50)  NOT NULL,
    amount            NUMERIC(10,2),
    currency          VARCHAR(3)   DEFAULT 'USD',
    status            VARCHAR(20)  NOT NULL,
    raw_payload       TEXT,                            -- full Square event for audit
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_events_order    ON payment_events(order_id);
CREATE INDEX idx_payment_events_sqpay   ON payment_events(square_payment_id);
