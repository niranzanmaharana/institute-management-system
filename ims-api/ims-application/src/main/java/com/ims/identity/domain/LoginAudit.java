package com.ims.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "login_audit")
public class LoginAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Column(length = 64)
  private String username;

  @Column(nullable = false)
  private boolean success;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected LoginAudit() {}

  public LoginAudit(
      Long userId, String username, boolean success, String ipAddress, String userAgent) {
    this.userId = userId;
    this.username = username;
    this.success = success;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
  }
}
