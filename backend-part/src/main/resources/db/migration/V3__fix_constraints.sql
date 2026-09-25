-- ===========================================================================
-- V3: Fix missing constraints identified during audit
--
-- 1. github_connection: add missing foreign key on user_id and enforce
--    the one-to-one relationship with a UNIQUE constraint (the JPA @OneToOne
--    mapping requires uniqueness at the DB level to be safe).
--
-- 2. Add indexes that were missing from V1 for github_connection.
-- ===========================================================================

-- Foreign key from github_connection.user_id -> users.id
-- (V1 created the column with no REFERENCES clause)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_github_connection_user'
          AND table_name = 'github_connection'
    ) THEN
        ALTER TABLE github_connection
            ADD CONSTRAINT fk_github_connection_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Unique constraint: one GitHub connection per application user
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uq_github_connection_user'
          AND table_name = 'github_connection'
    ) THEN
        ALTER TABLE github_connection
            ADD CONSTRAINT uq_github_connection_user UNIQUE (user_id);
    END IF;
END $$;

-- Index to speed up findByUser lookups on github_connection
CREATE INDEX IF NOT EXISTS idx_github_connection_user ON github_connection (user_id);
