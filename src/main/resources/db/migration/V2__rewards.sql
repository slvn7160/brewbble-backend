-- V2__rewards.sql – Reward points system

ALTER TABLE app_users
    ADD COLUMN reward_points INT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN reward_discount NUMERIC(10,2) NOT NULL DEFAULT 0;

CREATE TABLE reward_transactions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    order_id    BIGINT               REFERENCES orders(id)    ON DELETE SET NULL,
    type        VARCHAR(20) NOT NULL,   -- EARNED | REDEEMED
    points      INT         NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reward_tx_user ON reward_transactions(user_id);
