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
@Table(name = "course_fee_installments")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class CourseFeeInstallment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "fee_plan_id", nullable = false)
  private Long feePlanId;

  @Column(nullable = false)
  private int seq;

  @Column(name = "fee_category_id", nullable = false)
  private Long feeCategoryId;

  @Column(nullable = false, length = 128)
  private String label;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "due_offset_days", nullable = false)
  private int dueOffsetDays;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected CourseFeeInstallment() {}

  public static CourseFeeInstallment create(
      Long instituteId,
      Long feePlanId,
      int seq,
      Long feeCategoryId,
      String label,
      BigDecimal amount,
      int dueOffsetDays) {
    CourseFeeInstallment row = new CourseFeeInstallment();
    row.instituteId = instituteId;
    row.feePlanId = feePlanId;
    row.seq = seq;
    row.feeCategoryId = feeCategoryId;
    row.label = label.trim();
    row.amount = amount;
    row.dueOffsetDays = dueOffsetDays;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getFeePlanId() {
    return feePlanId;
  }

  public int getSeq() {
    return seq;
  }

  public Long getFeeCategoryId() {
    return feeCategoryId;
  }

  public String getLabel() {
    return label;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public int getDueOffsetDays() {
    return dueOffsetDays;
  }
}
