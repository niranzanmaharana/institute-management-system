-- Phase 5.2: rooms + weekly timetable slots with conflict detection support.

CREATE TABLE rooms (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  code            VARCHAR(32) NOT NULL,
  name            VARCHAR(128) NOT NULL,
  capacity        INT NOT NULL DEFAULT 0,
  status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_room_code (institute_id, code),
  KEY idx_rooms_institute (institute_id),
  CONSTRAINT fk_rooms_institute FOREIGN KEY (institute_id) REFERENCES institutes (id)
);

CREATE TABLE timetable_slots (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  institute_id    BIGINT NOT NULL,
  batch_id        BIGINT NOT NULL,
  subject_id      BIGINT NULL,
  faculty_id      BIGINT NULL,
  room_id         BIGINT NOT NULL,
  day_of_week     TINYINT NOT NULL COMMENT '1=Monday .. 7=Sunday',
  start_time      TIME NOT NULL,
  end_time        TIME NOT NULL,
  status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_tt_institute (institute_id),
  KEY idx_tt_batch (institute_id, batch_id, day_of_week),
  KEY idx_tt_room_day (institute_id, room_id, day_of_week, status),
  KEY idx_tt_faculty_day (institute_id, faculty_id, day_of_week, status),
  CONSTRAINT fk_tt_institute FOREIGN KEY (institute_id) REFERENCES institutes (id),
  CONSTRAINT fk_tt_batch FOREIGN KEY (batch_id) REFERENCES batches (id),
  CONSTRAINT fk_tt_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
  CONSTRAINT fk_tt_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

INSERT INTO permissions (code, description) VALUES
  ('timetable:write', 'Manage rooms and timetable slots');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ACADEMIC_COORDINATOR') AND p.code = 'timetable:write';
