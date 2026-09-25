-- ===========================================================================
-- Baseline of the pre-existing (Hibernate-managed) schema.
--
-- Spring Boot with spring-boot-starter-flyway historically ran Flyway with
-- <baseline-on-migrate>, so this V1 marker exists purely to anchor the
-- history. The tables below were already created by Hibernate (the app ran
-- with ddl-auto=create-drop before this migration existed); every statement
-- is guarded with IF NOT EXISTS so it is a no-op when they are present.
--
-- From V2 onward, the new CyberTotal tables are owned by Flyway, and
-- ddl-auto is now "validate": Hibernate must never silently diverge from
-- the migrated schema again.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS users (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255),
    email          VARCHAR(255),
    password       VARCHAR(255),
    enabled        BOOLEAN,
    created_at     TIMESTAMP,
    role           VARCHAR(255),
    active         BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN
);

CREATE TABLE IF NOT EXISTS project (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    user_id     BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    deleted     BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_project_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS target (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL,
    url         VARCHAR(255) NOT NULL,
    project_id  BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    deleted     BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_target_project FOREIGN KEY (project_id) REFERENCES project (id)
);

CREATE TABLE IF NOT EXISTS endpoint (
    id         BIGSERIAL PRIMARY KEY,
    target_id  BIGINT NOT NULL,
    path       VARCHAR(255) NOT NULL,
    method     VARCHAR(255) NOT NULL,
    url        VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    active     BOOLEAN NOT NULL,
    CONSTRAINT fk_endpoint_target FOREIGN KEY (target_id) REFERENCES target (id)
);

CREATE TABLE IF NOT EXISTS github_connection (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT,
    github_user_id BIGINT,
    github_username VARCHAR(255),
    access_token   VARCHAR(255) NOT NULL,
    expires_at     TIMESTAMP,
    connected_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked    BOOLEAN NOT NULL,
    user_id    BIGINT NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS email_verification_token (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    user_id    BIGINT NOT NULL,
    CONSTRAINT fk_email_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS forgot_password_reset_token (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_forgot_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_project_user ON project (user_id);
CREATE INDEX IF NOT EXISTS idx_target_project ON target (project_id);
CREATE INDEX IF NOT EXISTS idx_endpoint_target ON endpoint (target_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token (user_id);
