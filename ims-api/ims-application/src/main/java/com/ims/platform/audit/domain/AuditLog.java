package com.ims.platform.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id")
  private Long instituteId;

  @Column(nullable = false, length = 64)
  private String module;

  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "before_json", columnDefinition = "json")
  private String beforeJson;

  @Column(name = "after_json", columnDefinition = "json")
  private String afterJson;

  @Column(length = 512)
  private String reason;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "request_id", length = 64)
  private String requestId;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected AuditLog() {}

  public static AuditLog of(
      Long instituteId,
      String module,
      String entityType,
      Long entityId,
      String action,
      String beforeJson,
      String afterJson,
      String reason,
      Long actorUserId,
      String requestId) {
    AuditLog log = new AuditLog();
    log.instituteId = instituteId;
    log.module = module;
    log.entityType = entityType;
    log.entityId = entityId;
    log.action = action;
    log.beforeJson = beforeJson;
    log.afterJson = afterJson;
    log.reason = reason;
    log.actorUserId = actorUserId;
    log.requestId = requestId;
    log.createdAt = Instant.now();
    return log;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getAction() {
    return action;
  }

  public Long getEntityId() {
    return entityId;
  }
}
