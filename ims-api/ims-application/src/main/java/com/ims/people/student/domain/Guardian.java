package com.ims.people.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "guardians")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Guardian {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Guardian() {}

  public static Guardian create(Long instituteId, String name, String phone, String email) {
    Guardian guardian = new Guardian();
    guardian.instituteId = instituteId;
    guardian.name = name.trim();
    guardian.phone = blankToNull(phone);
    guardian.email = blankToNull(email);
    Instant now = Instant.now();
    guardian.createdAt = now;
    guardian.updatedAt = now;
    return guardian;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }
}
