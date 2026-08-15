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
@Table(name = "payments")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Payment {

  public static final String STATUS_SUCCESS = "SUCCESS";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "fee_account_id", nullable = false)
  private Long feeAccountId;

  @Column(name = "payment_no", nullable = false, length = 32)
  private String paymentNo;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 32)
  private String method = "CASH";

  @Column(name = "paid_at", nullable = false)
  private Instant paidAt;

  @Column(name = "idempotency_key", length = 64)
  private String idempotencyKey;

  @Column(nullable = false, length = 32)
  private String status = STATUS_SUCCESS;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Payment() {}

  public static Payment create(
      Long instituteId,
      Long feeAccountId,
      String paymentNo,
      BigDecimal amount,
      String method,
      String idempotencyKey) {
    Payment payment = new Payment();
    payment.instituteId = instituteId;
    payment.feeAccountId = feeAccountId;
    payment.paymentNo = paymentNo;
    payment.amount = amount;
    payment.method = method == null || method.isBlank() ? "CASH" : method.trim().toUpperCase();
    payment.paidAt = Instant.now();
    payment.idempotencyKey =
        idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    payment.status = STATUS_SUCCESS;
    Instant now = Instant.now();
    payment.createdAt = now;
    payment.updatedAt = now;
    return payment;
  }

  public void assignPaymentNo(String paymentNo) {
    this.paymentNo = paymentNo;
    this.updatedAt = Instant.now();
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

  public String getPaymentNo() {
    return paymentNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getMethod() {
    return method;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getStatus() {
    return status;
  }
}
