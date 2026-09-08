-- Additive migration: existing IDs, catalog and ratings are preserved.
ALTER TABLE person ADD COLUMN IF NOT EXISTS profile_path VARCHAR(500);
ALTER TABLE person ADD COLUMN IF NOT EXISTS biography TEXT;
ALTER TABLE content_actor ADD COLUMN IF NOT EXISTS cast_order INTEGER;

CREATE TABLE IF NOT EXISTS person_role (
    person_id BIGINT NOT NULL REFERENCES person(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ACTOR','DIRECTOR','CREATOR')),
    source VARCHAR(30) NOT NULL DEFAULT 'LOCAL_RELATION',
    PRIMARY KEY (person_id, role)
);
INSERT INTO person_role(person_id, role)
SELECT DISTINCT person_id, 'ACTOR' FROM content_actor ON CONFLICT DO NOTHING;
INSERT INTO person_role(person_id, role)
SELECT DISTINCT cd.person_id, CASE WHEN c.type = 'MOVIE' THEN 'DIRECTOR' ELSE 'CREATOR' END
FROM content_director cd JOIN content c ON c.id = cd.content_id ON CONFLICT DO NOTHING;

INSERT INTO app_user(name,email)
SELECT 'Gil','profile-gil@local.invalid' WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE lower(name)='gil') ON CONFLICT DO NOTHING;
INSERT INTO app_user(name,email)
SELECT 'Aliny','profile-aliny@local.invalid' WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE lower(name)='aliny') ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS user_model_state (
    user_id BIGINT PRIMARY KEY REFERENCES app_user(id),
    status VARCHAR(30) NOT NULL DEFAULT 'COLLECTING_DATA'
        CHECK (status IN ('COLLECTING_DATA','READY','TRAINING','TRAINED','DIRTY','ERROR')),
    revision BIGINT NOT NULL DEFAULT 0,
    trained_revision BIGINT NOT NULL DEFAULT 0,
    ratings_used_in_last_training INTEGER NOT NULL DEFAULT 0,
    trained_at TIMESTAMPTZ,
    training_data_at TIMESTAMPTZ,
    training_started_at TIMESTAMPTZ,
    train_loss DOUBLE PRECISION,
    validation_loss DOUBLE PRECISION,
    model_version INTEGER NOT NULL DEFAULT 0,
    model_path TEXT,
    auto_train_enabled BOOLEAN,
    last_error TEXT
);
INSERT INTO user_model_state(user_id) SELECT id FROM app_user ON CONFLICT DO NOTHING;
CREATE TABLE IF NOT EXISTS user_model_sample (
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    model_version INTEGER NOT NULL,
    rating_id BIGINT NOT NULL REFERENCES rating(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, model_version, rating_id)
);

-- Durable revision tracking includes updates/deletes and works inside rating transactions.
CREATE OR REPLACE FUNCTION track_rating_model_revision() RETURNS TRIGGER AS $$
DECLARE affected_user BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN affected_user := OLD.user_id; ELSE affected_user := NEW.user_id; END IF;
    INSERT INTO user_model_state(user_id, revision) VALUES (affected_user, 1)
    ON CONFLICT (user_id) DO UPDATE SET revision = user_model_state.revision + 1,
        status = CASE WHEN user_model_state.status = 'TRAINING' THEN 'TRAINING'
                      WHEN user_model_state.trained_at IS NOT NULL THEN 'DIRTY'
                      ELSE user_model_state.status END;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER rating_model_revision AFTER INSERT OR UPDATE OR DELETE ON rating
FOR EACH ROW EXECUTE FUNCTION track_rating_model_revision();

CREATE INDEX IF NOT EXISTS ix_outbox_unpublished ON rating_event_outbox(occurred_at) WHERE published_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_content_title_lower ON content(lower(title));
CREATE INDEX IF NOT EXISTS ix_person_name_lower ON person(lower(name));
CREATE INDEX IF NOT EXISTS ix_rating_user_created ON rating(user_id, created_at) WHERE content_id IS NOT NULL;
