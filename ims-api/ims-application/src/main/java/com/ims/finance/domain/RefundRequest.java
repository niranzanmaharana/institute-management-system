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
@Table(name = "refund_requests")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class RefundRequest {

  public static final String STATUS_REQUESTED = "REQUESTED";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";
  public static final String STATUS_PROCESSING = "PROCESSING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "fee_account_id", nullable = false)
  private Long feeAccountId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 32)
  private String status = STATUS_REQUESTED;

  @Column(nullable = false, length = 512)
  private String reason;

  @Column(name = "requested_by")
  private Long requestedBy;

  @Column(name = "decided_by")
  private Long decidedBy;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "process_note", length = 512)
  private String processNote;

  @Column(name = "process_idempotency_key", length = 64)
  private String processIdempotencyKey;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected RefundRequest() {}

  public static RefundRequest create(
      Long instituteId, Long feeAccountId, BigDecimal amount, String reason, Long requestedBy) {
    RefundRequest row = new RefundRequest();
    row.instituteId = instituteId;
    row.feeAccountId = feeAccountId;
    row.amount = amount;
    row.reason = reason.trim();
    row.status = STATUS_REQUESTED;
    row.requestedBy = requestedBy;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void approve(Long decidedBy) {
    requireStatus(STATUS_REQUESTED);
    this.status = STATUS_APPROVED;
    this.decidedBy = decidedBy;
    this.updatedAt = Instant.now();
  }

  public void reject(Long decidedBy, String reason) {
    requireStatus(STATUS_REQUESTED);
    this.status = STATUS_REJECTED;
    this.decidedBy = decidedBy;
    if (reason != null && !reason.isBlank()) {
      this.reason = reason.trim();
    }
    this.updatedAt = Instant.now();
  }

  public void markProcessing(String idempotencyKey) {
    requireStatus(STATUS_APPROVED);
    this.status = STATUS_PROCESSING;
    this.processIdempotencyKey = idempotencyKey;
    this.updatedAt = Instant.now();
  }

  public void complete(String note) {
    requireStatus(STATUS_PROCESSING);
    this.status = STATUS_COMPLETED;
    this.processedAt = Instant.now();
    this.processNote = note;
    this.updatedAt = this.processedAt;
  }

  public void fail(String note) {
    requireStatus(STATUS_PROCESSING);
    this.status = STATUS_FAILED;
    this.processedAt = Instant.now();
    this.processNote = note;
    this.updatedAt = this.processedAt;
  }

  private void requireStatus(String expected) {
    if (!expected.equals(this.status)) {
      throw new IllegalStateException("Refund is " + this.status + ", expected " + expected);
    }
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

  public BigDecimal getAmount() {
    return amount;
  }

  public String getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }

  public Long getRequestedBy() {
    return requestedBy;
  }

  public Long getDecidedBy() {
    return decidedBy;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public String getProcessNote() {
    return processNote;
  }

  public String getProcessIdempotencyKey() {
    return processIdempotencyKey;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
