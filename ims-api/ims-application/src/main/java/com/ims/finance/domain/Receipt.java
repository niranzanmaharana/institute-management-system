package com.ims.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "receipts")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Receipt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "payment_id", nullable = false)
  private Long paymentId;

  @Column(name = "receipt_no", nullable = false, length = 32)
  private String receiptNo;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected Receipt() {}

  public static Receipt create(Long instituteId, Long paymentId, String receiptNo) {
    Receipt receipt = new Receipt();
    receipt.instituteId = instituteId;
    receipt.paymentId = paymentId;
    receipt.receiptNo = receiptNo;
    Instant now = Instant.now();
    receipt.issuedAt = now;
    receipt.createdAt = now;
    return receipt;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }
}
