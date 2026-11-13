package com.ims.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "student_fee_accounts")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class StudentFeeAccount {

  public static final String STATUS_OPEN = "OPEN";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "enrollment_id", nullable = false)
  private Long enrollmentId;

  @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
  private String currency = "INR";

  @Column(nullable = false, length = 32)
  private String status = STATUS_OPEN;

  @Column(name = "outstanding_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal outstandingAmount = BigDecimal.ZERO;

  @Version
  @Column(nullable = false)
  private Long version = 0L;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected StudentFeeAccount() {}

  public static StudentFeeAccount create(
      Long instituteId, Long studentId, Long enrollmentId, String currency) {
    StudentFeeAccount account = new StudentFeeAccount();
    account.instituteId = instituteId;
    account.studentId = studentId;
    account.enrollmentId = enrollmentId;
    account.currency = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
    account.status = STATUS_OPEN;
    account.outstandingAmount = BigDecimal.ZERO.setScale(2);
    Instant now = Instant.now();
    account.createdAt = now;
    account.updatedAt = now;
    return account;
  }

  public void setOutstandingAmount(BigDecimal outstandingAmount) {
    this.outstandingAmount = outstandingAmount;
    this.updatedAt = Instant.now();
  }

  public void reduceOutstanding(BigDecimal amount) {
    this.outstandingAmount = this.outstandingAmount.subtract(amount);
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getStudentId() {
    return studentId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public String getCurrency() {
    return currency;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getOutstandingAmount() {
    return outstandingAmount;
  }

  public Long getVersion() {
    return version;
  }
}
