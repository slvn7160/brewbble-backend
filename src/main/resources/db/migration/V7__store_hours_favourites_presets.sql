-- V7__store_hours_favourites_presets.sql

-- ── Store hours ───────────────────────────────────────────────────────────
CREATE TABLE store_hours (
    day_of_week VARCHAR(10)  NOT NULL PRIMARY KEY,  -- MONDAY … SUNDAY
    open_time   TIME,                               -- null when closed all day
    close_time  TIME,
    is_closed   BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO store_hours (day_of_week, open_time, close_time) VALUES
    ('MONDAY',    '09:00', '21:00'),
    ('TUESDAY',   '09:00', '21:00'),
    ('WEDNESDAY', '09:00', '21:00'),
    ('THURSDAY',  '09:00', '21:00'),
    ('FRIDAY',    '09:00', '22:00'),
    ('SATURDAY',  '10:00', '22:00'),
    ('SUNDAY',    '10:00', '20:00');

-- ── User favourites ───────────────────────────────────────────────────────
CREATE TABLE user_favourites (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_users(id)  ON DELETE CASCADE,
    menu_item_id BIGINT       NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, menu_item_id)
);

CREATE INDEX idx_favourites_user ON user_favourites(user_id);

-- ── User presets ──────────────────────────────────────────────────────────
CREATE TABLE user_presets (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_users(id)  ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    menu_item_id BIGINT       REFERENCES menu_items(id)          ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Each preset can save multiple customization option IDs
CREATE TABLE user_preset_options (
    preset_id BIGINT NOT NULL REFERENCES user_presets(id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES customization_options(id) ON DELETE CASCADE,
    PRIMARY KEY (preset_id, option_id)
);

CREATE INDEX idx_presets_user ON user_presets(user_id);
