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
@Table(name = "fee_categories")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class FeeCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected FeeCategory() {}

  public static FeeCategory create(Long instituteId, String code, String name) {
    FeeCategory category = new FeeCategory();
    category.instituteId = instituteId;
    category.code = code.trim().toUpperCase();
    category.name = name.trim();
    Instant now = Instant.now();
    category.createdAt = now;
    category.updatedAt = now;
    return category;
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
}
