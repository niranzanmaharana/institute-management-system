package com.ims.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "template_code", nullable = false, length = 64)
  private String templateCode;

  @Column(nullable = false, length = 16)
  private String channel = NotificationChannel.EMAIL.name();

  @Column(name = "trigger_code", nullable = false, length = 64)
  private String triggerCode;

  @Column(nullable = false, length = 255)
  private String recipient;

  @Column(nullable = false, length = 512)
  private String subject;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(nullable = false, length = 32)
  private String status = NotificationDeliveryStatus.PENDING;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount = 0;

  @Column(name = "max_attempts", nullable = false)
  private int maxAttempts = 5;

  @Column(name = "last_error", length = 1024)
  private String lastError;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "related_entity_type", length = 64)
  private String relatedEntityType;

  @Column(name = "related_entity_id")
  private Long relatedEntityId;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected NotificationDelivery() {}

  public static NotificationDelivery create(
      Long instituteId,
      String templateCode,
      String channel,
      String triggerCode,
      String recipient,
      String subject,
      String body,
      String relatedEntityType,
      Long relatedEntityId,
      int maxAttempts) {
    NotificationDelivery row = new NotificationDelivery();
    row.instituteId = instituteId;
    row.templateCode = templateCode;
    row.channel = channel;
    row.triggerCode = triggerCode;
    row.recipient = recipient;
    row.subject = subject;
    row.body = body;
    row.relatedEntityType = relatedEntityType;
    row.relatedEntityId = relatedEntityId;
    row.maxAttempts = maxAttempts;
    row.status = NotificationDeliveryStatus.PENDING;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    row.nextRetryAt = now;
    return row;
  }

  public void markSent() {
    this.attemptCount++;
    this.status = NotificationDeliveryStatus.SENT;
    this.sentAt = Instant.now();
    this.lastError = null;
    this.nextRetryAt = null;
    this.updatedAt = Instant.now();
  }

  public void markSkipped(String reason) {
    this.status = NotificationDeliveryStatus.SKIPPED;
    this.lastError = trimError(reason);
    this.nextRetryAt = null;
    this.updatedAt = Instant.now();
  }

  public void markAttemptFailed(String error, Instant nextRetryAt, boolean exhausted) {
    this.attemptCount++;
    this.lastError = trimError(error);
    this.updatedAt = Instant.now();
    if (exhausted) {
      this.status = NotificationDeliveryStatus.FAILED;
      this.nextRetryAt = null;
    } else {
      this.status = NotificationDeliveryStatus.PENDING;
      this.nextRetryAt = nextRetryAt;
    }
  }

  private static String trimError(String error) {
    if (error == null) {
      return null;
    }
    String trimmed = error.trim();
    return trimmed.length() <= 1024 ? trimmed : trimmed.substring(0, 1024);
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public String getChannel() {
    return channel;
  }

  public String getTriggerCode() {
    return triggerCode;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getStatus() {
    return status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getNextRetryAt() {
    return nextRetryAt;
  }

  public String getRelatedEntityType() {
    return relatedEntityType;
  }

  public Long getRelatedEntityId() {
    return relatedEntityId;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}
