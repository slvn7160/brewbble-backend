-- V6__terminal_payments.sql – Square Terminal in-person payment support

ALTER TABLE orders
    ADD COLUMN payment_method       VARCHAR(20),
    ADD COLUMN terminal_checkout_id VARCHAR(100);

COMMENT ON COLUMN orders.payment_method IS
    'SQUARE_ONLINE=web checkout, SQUARE_TERMINAL=in-person card, CASH=cash payment';
COMMENT ON COLUMN orders.terminal_checkout_id IS
    'Square Terminal checkout ID used to track/cancel in-flight terminal charge';
