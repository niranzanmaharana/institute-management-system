-- Step 7.6: channel-abstract notifications (EMAIL first) — templates + delivery log.

CREATE TABLE notification_templates (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  code              VARCHAR(64) NOT NULL,
  channel           VARCHAR(16) NOT NULL DEFAULT 'EMAIL',
  name              VARCHAR(128) NOT NULL,
  subject_template  VARCHAR(512) NOT NULL,
  body_template     TEXT NOT NULL,
  active            TINYINT(1) NOT NULL DEFAULT 1,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_notification_templates_code_channel (code, channel)
);

INSERT INTO notification_templates (code, channel, name, subject_template, body_template, active) VALUES
  (
    'ADMISSION_APPROVED',
    'EMAIL',
    'Admission approved',
    'Admission approved — {{applicationNo}}',
    'Hello {{applicantName}},\n\nYour admission application {{applicationNo}} has been approved.\n\nRegards,\n{{instituteName}}',
    1
  ),
  (
    'PAYMENT_RECEIVED',
    'EMAIL',
    'Payment received',
    'Payment received — {{receiptNo}}',
    'Hello {{studentName}},\n\nWe received your payment of {{amount}} {{currency}}.\nReceipt: {{receiptNo}}\nPayment: {{paymentNo}}\n\nRegards,\n{{instituteName}}',
    1
  );

CREATE TABLE notification_deliveries (
  id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id          BIGINT NOT NULL,
  template_code         VARCHAR(64) NOT NULL,
  channel               VARCHAR(16) NOT NULL DEFAULT 'EMAIL',
  trigger_code          VARCHAR(64) NOT NULL,
  recipient             VARCHAR(255) NOT NULL,
  subject               VARCHAR(512) NOT NULL,
  body                  TEXT NOT NULL,
  status                VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  attempt_count         INT NOT NULL DEFAULT 0,
  max_attempts          INT NOT NULL DEFAULT 5,
  last_error            VARCHAR(1024) NULL,
  next_retry_at         DATETIME(6) NULL,
  related_entity_type   VARCHAR(64) NULL,
  related_entity_id     BIGINT NULL,
  sent_at               DATETIME(6) NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_notif_del_institute (institute_id),
  KEY idx_notif_del_status_retry (status, next_retry_at),
  KEY idx_notif_del_related (institute_id, related_entity_type, related_entity_id),
  CONSTRAINT fk_notif_del_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);
