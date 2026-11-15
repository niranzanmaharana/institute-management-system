package com.ims.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 16)
  private String channel = NotificationChannel.EMAIL.name();

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "subject_template", nullable = false, length = 512)
  private String subjectTemplate;

  @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
  private String bodyTemplate;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected NotificationTemplate() {}

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getChannel() {
    return channel;
  }

  public String getName() {
    return name;
  }

  public String getSubjectTemplate() {
    return subjectTemplate;
  }

  public String getBodyTemplate() {
    return bodyTemplate;
  }

  public boolean isActive() {
    return active;
  }
}
