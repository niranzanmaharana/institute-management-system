package com.ims.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id")
  private Long instituteId;

  @Column(nullable = false, length = 64)
  private String username;

  @Column(nullable = false)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "person_type", nullable = false, length = 32)
  private String personType = "NONE";

  @Column(name = "person_id")
  private Long personId;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  protected UserAccount() {}

  public UserAccount(
      Long instituteId, String username, String email, String passwordHash, Set<Role> roles) {
    this.instituteId = instituteId;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.roles = roles;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getStatus() {
    return status;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void markLogin() {
    this.lastLoginAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return "ACTIVE".equals(status) && deletedAt == null;
  }

  public void updateEmail(String email) {
    this.email = email;
    this.updatedAt = Instant.now();
  }

  public void replaceRoles(Set<Role> roles) {
    this.roles = new HashSet<>(roles);
    this.updatedAt = Instant.now();
  }

  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
    this.updatedAt = Instant.now();
  }

  public void deactivate() {
    this.status = "INACTIVE";
    this.updatedAt = Instant.now();
  }

  public void activate() {
    this.status = "ACTIVE";
    this.deletedAt = null;
    this.updatedAt = Instant.now();
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
