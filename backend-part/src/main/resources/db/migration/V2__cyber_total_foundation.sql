-- ===========================================================================
-- CyberTotal Phase 1 — foundation schema (Flyway)
--
-- Creates the new CyberTotal tables for the Sentinel, Agent Trap,
-- Intelligence and Alert modules. Existing tables (users, project, target,
-- endpoint, github_*) are NOT modified.
--
-- Notes:
--  * IF NOT EXISTS guards keep this migration safe in the current dev setup
--    where Hibernate ddl-auto=create-drop also manages the same schema.
--  * Ownership is never stored redundantly: every child resource chains to
--    its owning user through project -> target (existing relationships).
--  * No credentials, raw secrets, or personal data are stored in any column
--    defined here. JSONB is used only where flexible structured data is
--    genuinely required (signals, evidence, graph payloads, summaries).
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Engine 1 — Encrypted Sentinel
-- ---------------------------------------------------------------------------

-- One row per scan execution against a target.
CREATE TABLE IF NOT EXISTS sentinel_scan (
    id                BIGSERIAL PRIMARY KEY,
    target_id         BIGINT       NOT NULL,
    scan_type         VARCHAR(32)  NOT NULL,                -- MANUAL | SCHEDULED
    status            VARCHAR(32)  NOT NULL,                -- PENDING | RUNNING | COMPLETED | FAILED | CANCELLED
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    summary           JSONB,                                -- flexible scan summary (pages checked, counts)
    error_message     TEXT,                                 -- failure reason when status = FAILED
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_sentinel_scan_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT ck_sentinel_scan_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED'))
);
CREATE INDEX IF NOT EXISTS idx_sentinel_scan_target ON sentinel_scan (target_id);
CREATE INDEX IF NOT EXISTS idx_sentinel_scan_target_status ON sentinel_scan (target_id, status);

-- Aggregate result captured for each completed scan run.
CREATE TABLE IF NOT EXISTS scan_result (
    id                BIGSERIAL PRIMARY KEY,
    scan_id           BIGINT       NOT NULL,
    endpoints_checked INTEGER      NOT NULL DEFAULT 0,
    findings_count    INTEGER      NOT NULL DEFAULT 0,
    average_response_ms INTEGER,
    checked_at        TIMESTAMP    NOT NULL DEFAULT now(),
    details           JSONB,                                -- structured per-check rollup
    CONSTRAINT fk_scan_result_scan FOREIGN KEY (scan_id) REFERENCES sentinel_scan (id) ON DELETE CASCADE,
    CONSTRAINT uq_scan_result_scan UNIQUE (scan_id)         -- one result per scan
);

-- Individual security finding produced by a scan.
CREATE TABLE IF NOT EXISTS security_finding (
    id                BIGSERIAL PRIMARY KEY,
    scan_id           BIGINT       NOT NULL,
    target_id         BIGINT       NOT NULL,                -- denormalized for fast per-target queries
    endpoint_id       BIGINT,                               -- nullable: some findings are target-level
    check_type        VARCHAR(64)  NOT NULL,                -- e.g. SECURITY_HEADERS, TLS_CONFIG, AUTH_MISSING
    severity          VARCHAR(32)  NOT NULL,                -- INFO | LOW | MEDIUM | HIGH | CRITICAL
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    evidence          JSONB,                                -- sanitized observation only, never secrets
    remediation       TEXT,                                 -- actionable recommendation
    detected_at       TIMESTAMP    NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMP,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_finding_scan FOREIGN KEY (scan_id) REFERENCES sentinel_scan (id) ON DELETE CASCADE,
    CONSTRAINT fk_finding_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT fk_finding_endpoint FOREIGN KEY (endpoint_id) REFERENCES endpoint (id) ON DELETE SET NULL,
    CONSTRAINT ck_finding_severity CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL'))
);
CREATE INDEX IF NOT EXISTS idx_finding_scan ON security_finding (scan_id);
CREATE INDEX IF NOT EXISTS idx_finding_target_active ON security_finding (target_id, active);

-- Recurring scan schedule for a target.
CREATE TABLE IF NOT EXISTS scan_schedule (
    id                BIGSERIAL PRIMARY KEY,
    target_id         BIGINT       NOT NULL,
    cron_expression   VARCHAR(64)  NOT NULL,
    scan_type         VARCHAR(32)  NOT NULL DEFAULT 'SCHEDULED',
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    next_run_at       TIMESTAMP,
    last_run_at       TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_schedule_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_schedule_target ON scan_schedule (target_id);

-- ---------------------------------------------------------------------------
-- Engine 2 — Agent Trap
-- ---------------------------------------------------------------------------

-- One row per observed visitor session on a target.
CREATE TABLE IF NOT EXISTS agent_session (
    id                BIGSERIAL PRIMARY KEY,
    session_id        VARCHAR(64)  NOT NULL,                -- platform-generated opaque id
    target_id         BIGINT       NOT NULL,
    classification    VARCHAR(32),                          -- HUMAN | AUTOMATION | SUSPICIOUS | UNKNOWN (nullable until classified)
    confidence        DOUBLE PRECISION,                     -- 0.0 - 1.0
    signals           JSONB,                                -- contributing classification signals
    routing           VARCHAR(32)  NOT NULL DEFAULT 'REAL', -- REAL | DIGITAL_TWIN
    first_seen_at     TIMESTAMP    NOT NULL DEFAULT now(),
    last_seen_at      TIMESTAMP    NOT NULL DEFAULT now(),
    event_count       INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_agent_session_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_session_session_id UNIQUE (session_id),
    CONSTRAINT ck_agent_session_classification CHECK (
        classification IS NULL OR classification IN ('HUMAN','AUTOMATION','SUSPICIOUS','UNKNOWN')),
    CONSTRAINT ck_agent_session_routing CHECK (routing IN ('REAL','DIGITAL_TWIN'))
);
CREATE INDEX IF NOT EXISTS idx_agent_session_target ON agent_session (target_id);
CREATE INDEX IF NOT EXISTS idx_agent_session_target_seen ON agent_session (target_id, last_seen_at);

-- Ordered timeline of observable request events inside a session.
-- Security: metadata only — never credentials, cookies, or auth headers.
CREATE TABLE IF NOT EXISTS agent_event (
    id                BIGSERIAL PRIMARY KEY,
    agent_session_id  BIGINT       NOT NULL,
    target_id         BIGINT       NOT NULL,                -- denormalized for fast queries
    sequence_number   INTEGER      NOT NULL,                -- ordering within the session
    occurred_at       TIMESTAMP    NOT NULL DEFAULT now(),
    http_method       VARCHAR(16)  NOT NULL,
    path              VARCHAR(512) NOT NULL,                -- requested path only, no query secrets
    response_status   INTEGER,
    classification_signal VARCHAR(64),                     -- per-event signal, nullable
    routing           VARCHAR(32)  NOT NULL DEFAULT 'REAL',
    metadata          JSONB,                                -- sanitized request metadata only
    CONSTRAINT fk_agent_event_session FOREIGN KEY (agent_session_id) REFERENCES agent_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_event_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_event_session_sequence UNIQUE (agent_session_id, sequence_number)
);
CREATE INDEX IF NOT EXISTS idx_agent_event_session ON agent_event (agent_session_id);
CREATE INDEX IF NOT EXISTS idx_agent_event_target_time ON agent_event (target_id, occurred_at);

-- Historical classification result for a session (re-classifications appended).
CREATE TABLE IF NOT EXISTS agent_classification (
    id                BIGSERIAL PRIMARY KEY,
    agent_session_id  BIGINT       NOT NULL,
    category          VARCHAR(32)  NOT NULL,
    confidence        DOUBLE PRECISION NOT NULL,
    signals           JSONB,
    classified_at     TIMESTAMP    NOT NULL DEFAULT now(),
    classifier_version VARCHAR(32) NOT NULL DEFAULT 'rules-v1',
    CONSTRAINT fk_classification_session FOREIGN KEY (agent_session_id) REFERENCES agent_session (id) ON DELETE CASCADE,
    CONSTRAINT ck_classification_category CHECK (
        category IN ('HUMAN','AUTOMATION','SUSPICIOUS','UNKNOWN'))
);
CREATE INDEX IF NOT EXISTS idx_classification_session ON agent_classification (agent_session_id);

-- Canary sensors embedded into pages/twin responses.
CREATE TABLE IF NOT EXISTS canary (
    id                BIGSERIAL PRIMARY KEY,
    target_id         BIGINT       NOT NULL,
    canary_type       VARCHAR(32)  NOT NULL,                -- e.g. HIDDEN_LINK | DECOY_PATH | SYNTHETIC_TOKEN
    identifier        VARCHAR(128) NOT NULL,                -- unique, non-sensitive marker value
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    last_triggered_at TIMESTAMP,
    trigger_count     INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_canary_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT uq_canary_identifier UNIQUE (identifier)
);
CREATE INDEX IF NOT EXISTS idx_canary_target ON canary (target_id);

-- Isolated synthetic environment per target (Agent Trap Digital Twin).
CREATE TABLE IF NOT EXISTS digital_twin (
    id                BIGSERIAL PRIMARY KEY,
    target_id         BIGINT       NOT NULL,
    twin_key          VARCHAR(64)  NOT NULL,                -- opaque routing key used in local demo routing
    base_path         VARCHAR(255) NOT NULL,                -- local synthetic mount path
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_twin_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT uq_twin_target UNIQUE (target_id),
    CONSTRAINT uq_twin_key UNIQUE (twin_key)
);

-- Synthetic decoy resources served by the twin.
CREATE TABLE IF NOT EXISTS decoy_resource (
    id                BIGSERIAL PRIMARY KEY,
    digital_twin_id   BIGINT       NOT NULL,
    path              VARCHAR(512) NOT NULL,
    resource_type     VARCHAR(32)  NOT NULL,                -- e.g. FAKE_DOCUMENT | FAKE_API | FAKE_RECORD
    content           TEXT,                                 -- synthetic content only
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_decoy_twin FOREIGN KEY (digital_twin_id) REFERENCES digital_twin (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_decoy_twin ON decoy_resource (digital_twin_id);

-- ---------------------------------------------------------------------------
-- Engine 3 — Agent Intelligence Network
-- ---------------------------------------------------------------------------

-- Anonymized behavioral fingerprint shared across the intelligence network.
CREATE TABLE IF NOT EXISTS behavior_fingerprint (
    id                BIGSERIAL PRIMARY KEY,
    fingerprint_hash  VARCHAR(64)  NOT NULL,                -- deterministic hash of normalized sequence
    sequence          JSONB        NOT NULL,                -- normalized behavior sequence (no sensitive data)
    signals           JSONB,                                -- observable signals summary
    first_seen_at     TIMESTAMP    NOT NULL,
    last_seen_at      TIMESTAMP    NOT NULL,
    reporting_sites   INTEGER      NOT NULL DEFAULT 1,      -- distinct protected sites reporting this fingerprint
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_fingerprint_hash UNIQUE (fingerprint_hash)
);
CREATE INDEX IF NOT EXISTS idx_fingerprint_hash ON behavior_fingerprint (fingerprint_hash);

-- Directed behavioral graph nodes for a session.
CREATE TABLE IF NOT EXISTS behavior_node (
    id                BIGSERIAL PRIMARY KEY,
    agent_session_id  BIGINT       NOT NULL,
    node_key          VARCHAR(128) NOT NULL,                -- stable node identity within the graph
    node_type         VARCHAR(32)  NOT NULL,                -- e.g. REQUEST | CANARY_HIT | AUTH_FAILURE
    label             VARCHAR(255),
    risk_contribution DOUBLE PRECISION NOT NULL DEFAULT 0,
    first_seen_at     TIMESTAMP    NOT NULL DEFAULT now(),
    last_seen_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_node_session FOREIGN KEY (agent_session_id) REFERENCES agent_session (id) ON DELETE CASCADE,
    CONSTRAINT uq_behavior_node UNIQUE (agent_session_id, node_key)
);
CREATE INDEX IF NOT EXISTS idx_node_session ON behavior_node (agent_session_id);

-- Directed edges between behavior nodes (observed transitions).
CREATE TABLE IF NOT EXISTS behavior_edge (
    id                BIGSERIAL PRIMARY KEY,
    agent_session_id  BIGINT       NOT NULL,
    source_node_id    BIGINT       NOT NULL,
    target_node_id    BIGINT       NOT NULL,
    transition_count  INTEGER      NOT NULL DEFAULT 1,
    first_seen_at     TIMESTAMP    NOT NULL DEFAULT now(),
    last_seen_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_edge_session FOREIGN KEY (agent_session_id) REFERENCES agent_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_edge_source FOREIGN KEY (source_node_id) REFERENCES behavior_node (id) ON DELETE CASCADE,
    CONSTRAINT fk_edge_target FOREIGN KEY (target_node_id) REFERENCES behavior_node (id) ON DELETE CASCADE,
    CONSTRAINT uq_behavior_edge UNIQUE (agent_session_id, source_node_id, target_node_id)
);
CREATE INDEX IF NOT EXISTS idx_edge_session ON behavior_edge (agent_session_id);

-- Threat cluster grouping similar fingerprints.
CREATE TABLE IF NOT EXISTS threat_cluster (
    id                BIGSERIAL PRIMARY KEY,
    cluster_key       VARCHAR(64)  NOT NULL,
    label             VARCHAR(255),
    member_count      INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_threat_cluster_key UNIQUE (cluster_key)
);

-- Membership of fingerprints in clusters.
CREATE TABLE IF NOT EXISTS threat_cluster_member (
    id                BIGSERIAL PRIMARY KEY,
    threat_cluster_id BIGINT       NOT NULL,
    fingerprint_id    BIGINT       NOT NULL,
    joined_at         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_member_cluster FOREIGN KEY (threat_cluster_id) REFERENCES threat_cluster (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_fingerprint FOREIGN KEY (fingerprint_id) REFERENCES behavior_fingerprint (id) ON DELETE CASCADE,
    CONSTRAINT uq_cluster_member UNIQUE (threat_cluster_id, fingerprint_id)
);

-- Explainable risk score computed for a session.
CREATE TABLE IF NOT EXISTS threat_score (
    id                BIGSERIAL PRIMARY KEY,
    agent_session_id  BIGINT       NOT NULL,
    score             INTEGER      NOT NULL,                -- 0-100
    category          VARCHAR(32)  NOT NULL,                -- e.g. BENIGN | LOW | ELEVATED | HIGH | CRITICAL
    confidence        DOUBLE PRECISION NOT NULL,
    breakdown         JSONB        NOT NULL,                -- per-signal contribution (explainability)
    known_behavior    BOOLEAN      NOT NULL DEFAULT FALSE,  -- seen before in the network?
    recommended_action VARCHAR(64),                        -- e.g. MONITOR | REDIRECT_TO_TWIN | BLOCK
    scored_at         TIMESTAMP    NOT NULL DEFAULT now(),
    scorer_version    VARCHAR(32)  NOT NULL DEFAULT 'rules-v1',
    CONSTRAINT fk_score_session FOREIGN KEY (agent_session_id) REFERENCES agent_session (id) ON DELETE CASCADE,
    CONSTRAINT ck_score_range CHECK (score BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_score_session ON threat_score (agent_session_id);

-- ---------------------------------------------------------------------------
-- Alerts and remediation
-- ---------------------------------------------------------------------------

-- Alert raised for a target (from findings or intelligence signals).
CREATE TABLE IF NOT EXISTS security_alert (
    id                BIGSERIAL PRIMARY KEY,
    target_id         BIGINT       NOT NULL,
    agent_session_id  BIGINT,                               -- nullable: alerts may originate from scans
    security_finding_id BIGINT,                             -- nullable: alerts may originate from sessions
    severity          VARCHAR(32)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    message           TEXT,
    status            VARCHAR(32)  NOT NULL DEFAULT 'OPEN', -- OPEN | ACKNOWLEDGED | RESOLVED
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMP,
    CONSTRAINT fk_alert_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_session FOREIGN KEY (agent_session_id) REFERENCES agent_session (id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_finding FOREIGN KEY (security_finding_id) REFERENCES security_finding (id) ON DELETE SET NULL,
    CONSTRAINT ck_alert_severity CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_alert_status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED'))
);
CREATE INDEX IF NOT EXISTS idx_alert_target_status ON security_alert (target_id, status);

-- Actionable remediation advice attached to a finding.
CREATE TABLE IF NOT EXISTS remediation_recommendation (
    id                BIGSERIAL PRIMARY KEY,
    security_finding_id BIGINT       NOT NULL,
    recommendation    TEXT         NOT NULL,
    priority          INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_remediation_finding FOREIGN KEY (security_finding_id) REFERENCES security_finding (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_remediation_finding ON remediation_recommendation (security_finding_id);

-- Incident report aggregating alerts for later export/review.
CREATE TABLE IF NOT EXISTS incident_report (
    id                BIGSERIAL PRIMARY KEY,
    target_id         BIGINT       NOT NULL,
    title             VARCHAR(255) NOT NULL,
    summary           TEXT,
    details           JSONB,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_incident_target FOREIGN KEY (target_id) REFERENCES target (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_incident_target ON incident_report (target_id);
