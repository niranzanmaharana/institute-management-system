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
@Table(name = "payment_allocations")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class PaymentAllocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "payment_id", nullable = false)
  private Long paymentId;

  @Column(name = "invoice_id", nullable = false)
  private Long invoiceId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected PaymentAllocation() {}

  public static PaymentAllocation create(
      Long instituteId, Long paymentId, Long invoiceId, BigDecimal amount) {
    PaymentAllocation allocation = new PaymentAllocation();
    allocation.instituteId = instituteId;
    allocation.paymentId = paymentId;
    allocation.invoiceId = invoiceId;
    allocation.amount = amount;
    allocation.createdAt = Instant.now();
    return allocation;
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

  public Long getInvoiceId() {
    return invoiceId;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
