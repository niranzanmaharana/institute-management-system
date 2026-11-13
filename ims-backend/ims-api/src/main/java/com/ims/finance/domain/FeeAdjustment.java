package com.ims.finance.domain;

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
@Table(name = "fee_adjustments")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class FeeAdjustment {

  public static final String TYPE_DISCOUNT = "DISCOUNT";
  public static final String TYPE_SCHOLARSHIP = "SCHOLARSHIP";
  public static final String TYPE_WAIVER = "WAIVER";
  public static final String TYPE_PENALTY = "PENALTY";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "fee_account_id", nullable = false)
  private Long feeAccountId;

  @Column(nullable = false, length = 32)
  private String type;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 512)
  private String reason;

  @Column(name = "approved_by")
  private Long approvedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected FeeAdjustment() {}

  public static FeeAdjustment create(
      Long instituteId,
      Long feeAccountId,
      String type,
      BigDecimal amount,
      String reason,
      Long approvedBy) {
    FeeAdjustment row = new FeeAdjustment();
    row.instituteId = instituteId;
    row.feeAccountId = feeAccountId;
    row.type = type;
    row.amount = amount;
    row.reason = reason.trim();
    row.approvedBy = approvedBy;
    row.createdAt = Instant.now();
    return row;
  }

  public static boolean isCredit(String type) {
    return TYPE_DISCOUNT.equals(type)
        || TYPE_SCHOLARSHIP.equals(type)
        || TYPE_WAIVER.equals(type);
  }

  public static String normalizeType(String type) {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type is required");
    }
    String normalized = type.trim().toUpperCase();
    if (!TYPE_DISCOUNT.equals(normalized)
        && !TYPE_SCHOLARSHIP.equals(normalized)
        && !TYPE_WAIVER.equals(normalized)
        && !TYPE_PENALTY.equals(normalized)) {
      throw new IllegalArgumentException("Invalid adjustment type");
    }
    return normalized;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getFeeAccountId() {
    return feeAccountId;
  }

  public String getType() {
    return type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getReason() {
    return reason;
  }

  public Long getApprovedBy() {
    return approvedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
