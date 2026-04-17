-- V5__customizations.sql – Drink customization support

-- Customization options catalogue (managed by admin)
CREATE TABLE customization_options (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    type         VARCHAR(30)  NOT NULL,  -- SWEETNESS | ICE_LEVEL | MILK_TYPE | TOPPING | SIZE | TEMPERATURE
    price_delta  NUMERIC(6,2) NOT NULL DEFAULT 0.00,
    available    BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Customizations chosen per order line item
CREATE TABLE order_item_customizations (
    id             BIGSERIAL PRIMARY KEY,
    order_item_id  BIGINT       NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    option_id      BIGINT                REFERENCES customization_options(id) ON DELETE SET NULL,
    name           VARCHAR(100) NOT NULL,   -- snapshot at time of order
    type           VARCHAR(30)  NOT NULL,
    price_delta    NUMERIC(6,2) NOT NULL DEFAULT 0.00
);

CREATE INDEX idx_oic_order_item ON order_item_customizations(order_item_id);

-- Seed default options
INSERT INTO customization_options (name, type, price_delta, sort_order) VALUES
  -- Sweetness
  ('0% Sugar',    'SWEETNESS', 0.00, 1),
  ('25% Sugar',   'SWEETNESS', 0.00, 2),
  ('50% Sugar',   'SWEETNESS', 0.00, 3),
  ('75% Sugar',   'SWEETNESS', 0.00, 4),
  ('100% Sugar',  'SWEETNESS', 0.00, 5),
  -- Ice
  ('No Ice',      'ICE_LEVEL', 0.00, 1),
  ('Less Ice',    'ICE_LEVEL', 0.00, 2),
  ('Regular Ice', 'ICE_LEVEL', 0.00, 3),
  ('Extra Ice',   'ICE_LEVEL', 0.00, 4),
  -- Milk
  ('Whole Milk',   'MILK_TYPE', 0.00, 1),
  ('Oat Milk',     'MILK_TYPE', 0.50, 2),
  ('Almond Milk',  'MILK_TYPE', 0.50, 3),
  ('Coconut Milk', 'MILK_TYPE', 0.50, 4),
  ('No Milk',      'MILK_TYPE', 0.00, 5),
  -- Toppings
  ('Tapioca Pearls', 'TOPPING', 0.75, 1),
  ('Popping Boba',   'TOPPING', 0.75, 2),
  ('Pudding',        'TOPPING', 0.75, 3),
  ('Coconut Jelly',  'TOPPING', 0.75, 4),
  ('Aloe Vera',      'TOPPING', 0.75, 5),
  ('Cheese Foam',    'TOPPING', 1.00, 6),
  ('Red Bean',       'TOPPING', 0.75, 7),
  -- Size
  ('Regular (16oz)', 'SIZE', 0.00, 1),
  ('Large (24oz)',   'SIZE', 1.00, 2),
  -- Temperature
  ('Iced', 'TEMPERATURE', 0.00, 1),
  ('Hot',  'TEMPERATURE', 0.00, 2);
