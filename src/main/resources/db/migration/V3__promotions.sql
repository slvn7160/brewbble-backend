-- V3__promotions.sql – Promotion discounts

CREATE TABLE promotions (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(50)     NOT NULL UNIQUE,
    description      TEXT,
    type             VARCHAR(20)     NOT NULL,   -- PERCENTAGE | FIXED_AMOUNT
    value            NUMERIC(10,2)   NOT NULL,
    active           BOOLEAN         NOT NULL DEFAULT TRUE,
    min_order_amount NUMERIC(10,2),              -- nullable — no minimum if null
    valid_from       TIMESTAMPTZ,                -- nullable — open start if null
    valid_until      TIMESTAMPTZ,                -- nullable — never expires if null
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

ALTER TABLE orders
    ADD COLUMN promo_discount  NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN promotion_id    BIGINT REFERENCES promotions(id) ON DELETE SET NULL;
