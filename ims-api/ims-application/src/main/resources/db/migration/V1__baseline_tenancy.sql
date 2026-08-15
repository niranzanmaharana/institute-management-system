-- Flyway conventions:
-- 1. Every business table MUST include institute_id BIGINT NOT NULL (except institutes itself and pure platform tables).
-- 2. Unique business codes are tenant-scoped: UNIQUE (institute_id, code).
-- 3. No cross-module foreign keys (enforced by application ownership, not DB FKs across modules).

CREATE TABLE institutes (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  code       VARCHAR(32) NOT NULL,
  name       VARCHAR(255) NOT NULL,
  status     VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  timezone   VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_institutes_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id  BIGINT NULL,
  module        VARCHAR(64) NOT NULL,
  entity_type   VARCHAR(64) NOT NULL,
  entity_id     BIGINT NOT NULL,
  action        VARCHAR(64) NOT NULL,
  before_json   JSON NULL,
  after_json    JSON NULL,
  reason        VARCHAR(512) NULL,
  actor_user_id BIGINT NULL,
  request_id    VARCHAR(64) NULL,
  ip_address    VARCHAR(64) NULL,
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_audit_institute_entity (institute_id, entity_type, entity_id),
  KEY idx_audit_actor (actor_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE idempotency_records (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id     BIGINT NOT NULL,
  idempotency_key  VARCHAR(128) NOT NULL,
  user_id          BIGINT NOT NULL,
  operation        VARCHAR(64) NOT NULL,
  request_hash     VARCHAR(128) NOT NULL,
  response_status  INT NOT NULL,
  response_body    JSON NOT NULL,
  expires_at       DATETIME(6) NOT NULL,
  created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_idem (institute_id, user_id, operation, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Phase 0 probe table for tenant isolation harness (not a product domain aggregate).
CREATE TABLE tenant_probes (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id BIGINT NOT NULL,
  label        VARCHAR(128) NOT NULL,
  created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_tenant_probes_institute (institute_id),
  CONSTRAINT fk_tenant_probes_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
