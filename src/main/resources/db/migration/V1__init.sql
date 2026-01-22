-- =========================
-- 1. users
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL
);

-- =========================
-- 2. body_part enum
-- =========================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'body_part') THEN
        CREATE TYPE body_part AS ENUM (
            'CHEST','SHOULDER','BACK','ARMS','LEGS','GLUTES','UPPER_BODY','LOWER_BODY','FULL_BODY'
        );
    END IF;
END$$;

-- =========================
-- 3. exercises
-- =========================
CREATE TABLE IF NOT EXISTS exercises (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    body_part body_part NOT NULL
);

-- 共通：user_id IS NULL のとき lower(name) をユニーク
CREATE UNIQUE INDEX IF NOT EXISTS ux_exercises_common_name
ON exercises (lower(name))
WHERE user_id IS NULL;

-- ユーザー：user_id IS NOT NULL のとき (user_id, lower(name)) をユニーク
CREATE UNIQUE INDEX IF NOT EXISTS ux_exercises_user_name
ON exercises (user_id, lower(name))
WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_exercises_user_id ON exercises (user_id);
CREATE INDEX IF NOT EXISTS ix_exercises_body_part ON exercises (body_part);

-- =========================
-- 4. workouts
-- =========================
CREATE TABLE IF NOT EXISTS workouts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workout_date DATE NOT NULL,
    note VARCHAR(500),
    CONSTRAINT uk_workouts_user_date UNIQUE (user_id, workout_date)
);

CREATE INDEX IF NOT EXISTS ix_workouts_user_date_desc ON workouts (user_id, workout_date DESC);

-- =========================
-- 5. workout_items（sets無し）
-- =========================
CREATE TABLE IF NOT EXISTS workout_items (
    id BIGSERIAL PRIMARY KEY,
    workout_id BIGINT NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id),
    body_part body_part NOT NULL,
    weight NUMERIC(6,2),
    reps INTEGER,
    CONSTRAINT ck_workout_items_weight_nonneg CHECK (weight IS NULL OR weight >= 0),
    CONSTRAINT ck_workout_items_reps_nonneg CHECK (reps IS NULL OR reps >= 0)
);

CREATE INDEX IF NOT EXISTS ix_workout_items_workout_id ON workout_items (workout_id);
CREATE INDEX IF NOT EXISTS ix_workout_items_exercise_id ON workout_items (exercise_id);
CREATE INDEX IF NOT EXISTS ix_workout_items_body_part ON workout_items (body_part);

-- =========================
-- 6. 共通種目 seed（user_id NULL）
--   ※部分ユニークがあるので "NOT EXISTS" で安全に投入
-- =========================
INSERT INTO exercises (user_id, name, body_part)
SELECT NULL, v.name, v.body_part::body_part
FROM (
  VALUES
    ('ベンチプレス', 'CHEST'),
    ('インクラインベンチプレス', 'CHEST'),
    ('ダンベルプレス', 'CHEST'),
    ('ショルダープレス', 'SHOULDER'),
    ('サイドレイズ', 'SHOULDER'),
    ('懸垂（チンニング）', 'BACK'),
    ('ラットプルダウン', 'BACK'),
    ('アームカール', 'ARMS'),
    ('ハンマーカール', 'ARMS'),
    ('スクワット', 'LEGS'),
    ('レッグプレス', 'LEGS'),
    ('ヒップスラスト', 'GLUTES')
) AS v(name, body_part)
WHERE NOT EXISTS (
  SELECT 1 FROM exercises e
  WHERE e.user_id IS NULL AND lower(e.name) = lower(v.name)
);
