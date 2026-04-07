-- V1__init.sql  –  Brewbble baseline schema

-- ── USERS ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app_users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)        NOT NULL,
    email      VARCHAR(150)        NOT NULL UNIQUE,
    password   VARCHAR(255)        NOT NULL,
    role       VARCHAR(20)         NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

-- ── MENU ITEMS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_items (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)     NOT NULL,
    description    TEXT,
    price          NUMERIC(8,2)     NOT NULL,
    category       VARCHAR(50)      NOT NULL,
    image_url      VARCHAR(500),
    badge          VARCHAR(50),
    rating         NUMERIC(3,2)     DEFAULT 0,
    review_count   INT              DEFAULT 0,
    available      BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- ── ORDERS ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT           REFERENCES app_users(id) ON DELETE SET NULL,
    status         VARCHAR(30)      NOT NULL DEFAULT 'PENDING',
    subtotal       NUMERIC(10,2)    NOT NULL,
    tax            NUMERIC(10,2)    NOT NULL,
    delivery_fee   NUMERIC(10,2)    NOT NULL DEFAULT 0,
    total          NUMERIC(10,2)    NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- ── ORDER LINE ITEMS ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id   BIGINT           REFERENCES menu_items(id) ON DELETE SET NULL,
    name           VARCHAR(100)     NOT NULL,  -- snapshot at time of order
    unit_price     NUMERIC(8,2)     NOT NULL,
    quantity       INT              NOT NULL CHECK (quantity > 0),
    subtotal       NUMERIC(10,2)    NOT NULL
);

-- ── INDEXES ──────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_orders_user_id    ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_menu_category     ON menu_items(category);

-- ── SEED: Menu items ─────────────────────────────────────
INSERT INTO menu_items (name, description, price, category, image_url, badge, rating, review_count) VALUES
  ('Classic Milk Tea',       'Smooth and creamy with bold black tea and fresh milk',                      5.99, 'MILK_TEA',  '/classic-milk-tea.jpeg', 'Best Seller',  4.8, 124),
  ('Taro Milk Tea',          'Velvety purple taro with a sweet, nutty finish',                            6.49, 'MILK_TEA',  '/taro-milk-tea.jpeg',    'Fan Favorite', 4.9,  98),
  ('Brown Sugar Boba',       'Tiger-stripe brown sugar syrup swirled through fresh milk with chewy boba', 6.99, 'MILK_TEA',  '/classic-milk-tea.jpeg', 'Trending',     4.9, 143),
  ('Matcha Milk Tea',        'Ceremonial grade matcha blended with creamy oat milk and a touch of honey', 6.49, 'MILK_TEA',  '/taro-milk-tea.jpeg',    NULL,           4.7,  87),
  ('Mango Tea',              'Tropical mango bursting with fresh fruit flavors',                          5.49, 'FRUIT_TEA', '/mango-tea.jpeg',         NULL,           4.7,  76),
  ('Strawberry Lemonade Tea','Tangy fresh lemonade meets sweet strawberry in a light jasmine tea base',   5.99, 'FRUIT_TEA', '/classic-milk-tea.jpeg', 'New',          4.6,  54),
  ('Passion Fruit Green Tea','Zesty passion fruit with a light grassy green tea and popping pearls',      5.49, 'FRUIT_TEA', '/taro-milk-tea.jpeg',     NULL,           4.8,  62),
  ('Lychee Slush',           'Crushed ice blended with sweet lychee syrup and chewy coconut jelly',      5.99, 'SLUSHIE',   '/taro-milk-tea.jpeg',    'New',          4.6,  41),
  ('Watermelon Mint Slush',  'Fresh watermelon blended with garden mint over a mountain of crushed ice',  5.49, 'SLUSHIE',   '/mango-tea.jpeg',         NULL,           4.7,  38),
  ('Peach Oolong Slush',     'Floral oolong tea meets ripe summer peach in a frosty blended treat',      6.49, 'SLUSHIE',   '/classic-milk-tea.jpeg', 'Staff Pick',   4.8,  29)
ON CONFLICT DO NOTHING;
