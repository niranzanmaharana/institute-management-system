package com.ims.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "courses")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(length = 512)
  private String description;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Course() {}

  public static Course create(Long instituteId, String code, String name, String description) {
    Course course = new Course();
    course.instituteId = instituteId;
    course.code = code.trim().toUpperCase();
    course.name = name.trim();
    course.description = blankToNull(description);
    course.status = "ACTIVE";
    Instant now = Instant.now();
    course.createdAt = now;
    course.updatedAt = now;
    return course;
  }

  public void update(String name, String description) {
    this.name = name.trim();
    this.description = blankToNull(description);
    this.updatedAt = Instant.now();
  }

  public void deactivate() {
    this.status = "INACTIVE";
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return "ACTIVE".equals(status) && deletedAt == null;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getStatus() {
    return status;
  }
}
