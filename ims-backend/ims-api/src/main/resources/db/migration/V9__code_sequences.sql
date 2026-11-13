-- Per-tenant (and platform) monotonic counters for business codes.
-- institute_id = 0 is the platform scope (e.g. INSTITUTE codes).
CREATE TABLE code_sequences (
  institute_id BIGINT       NOT NULL,
  entity_type  VARCHAR(32)  NOT NULL,
  next_value   BIGINT       NOT NULL,
  PRIMARY KEY (institute_id, entity_type)
);
