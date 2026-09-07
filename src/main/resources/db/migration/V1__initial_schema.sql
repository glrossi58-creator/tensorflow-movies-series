CREATE TABLE IF NOT EXISTS content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    tmdb_id INTEGER,
    type VARCHAR(20) NOT NULL,
    overview TEXT,
    release_date DATE,
    poster_path VARCHAR(500),
    CONSTRAINT content_type_check CHECK (type IN ('MOVIE', 'SERIES'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_content_tmdb_type ON content (tmdb_id, type);

CREATE TABLE IF NOT EXISTS genre (
    id BIGSERIAL PRIMARY KEY,
    tmdb_id INTEGER NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS person (
    id BIGSERIAL PRIMARY KEY,
    tmdb_id INTEGER NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS content_genre (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre(id),
    CONSTRAINT uq_content_genre UNIQUE (content_id, genre_id)
);

CREATE TABLE IF NOT EXISTS content_actor (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES person(id),
    CONSTRAINT uq_content_actor UNIQUE (content_id, person_id)
);

CREATE TABLE IF NOT EXISTS content_director (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES person(id),
    CONSTRAINT uq_content_director UNIQUE (content_id, person_id)
);

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email_lower ON app_user (lower(email));

CREATE TABLE IF NOT EXISTS rating (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    value SMALLINT NOT NULL,
    content_id BIGINT REFERENCES content(id) ON DELETE CASCADE,
    person_id BIGINT REFERENCES person(id) ON DELETE CASCADE,
    genre_id BIGINT REFERENCES genre(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT rating_value_check CHECK (value BETWEEN 1 AND 5),
    CONSTRAINT rating_target_type_check CHECK (target_type IN ('MOVIE','SERIES','ACTOR','DIRECTOR','CREATOR','GENRE')),
    CONSTRAINT rating_target_shape_check CHECK (
        (target_type IN ('MOVIE','SERIES') AND content_id IS NOT NULL AND person_id IS NULL AND genre_id IS NULL) OR
        (target_type IN ('ACTOR','DIRECTOR','CREATOR') AND content_id IS NULL AND person_id IS NOT NULL AND genre_id IS NULL) OR
        (target_type = 'GENRE' AND content_id IS NULL AND person_id IS NULL AND genre_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_rating_user_content ON rating (user_id, target_type, content_id) WHERE content_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_rating_user_person ON rating (user_id, target_type, person_id) WHERE person_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_rating_user_genre ON rating (user_id, target_type, genre_id) WHERE genre_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_rating_user ON rating (user_id);

