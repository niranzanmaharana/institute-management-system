package com.ims.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "course_fee_plans")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class CourseFeePlan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
  private String currency = "INR";

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected CourseFeePlan() {}

  public static CourseFeePlan create(
      Long instituteId,
      Long courseId,
      String code,
      String name,
      String currency,
      BigDecimal totalAmount) {
    CourseFeePlan plan = new CourseFeePlan();
    plan.instituteId = instituteId;
    plan.courseId = courseId;
    plan.code = code.trim().toUpperCase();
    plan.name = name.trim();
    plan.currency = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
    plan.totalAmount = totalAmount;
    plan.status = "ACTIVE";
    Instant now = Instant.now();
    plan.createdAt = now;
    plan.updatedAt = now;
    return plan;
  }

  public void update(String name, BigDecimal totalAmount) {
    this.name = name.trim();
    this.totalAmount = totalAmount;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getStatus() {
    return status;
  }
}
