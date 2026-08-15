package com.ims.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "invoices")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Invoice {

  public static final String STATUS_DUE = "DUE";
  public static final String STATUS_PARTIAL = "PARTIAL";
  public static final String STATUS_PAID = "PAID";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "fee_account_id", nullable = false)
  private Long feeAccountId;

  @Column(name = "invoice_no", nullable = false, length = 32)
  private String invoiceNo;

  @Column(name = "fee_category_id", nullable = false)
  private Long feeCategoryId;

  @Column(name = "financial_year_id", nullable = false)
  private Long financialYearId;

  @Column(nullable = false, length = 255)
  private String description;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
  private BigDecimal amountPaid = BigDecimal.ZERO;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, length = 32)
  private String status = STATUS_DUE;

  @Column(name = "installment_no")
  private Integer installmentNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Invoice() {}

  public static Invoice create(
      Long instituteId,
      Long feeAccountId,
      String invoiceNo,
      Long feeCategoryId,
      Long financialYearId,
      String description,
      BigDecimal amount,
      LocalDate dueDate,
      Integer installmentNo) {
    Invoice invoice = new Invoice();
    invoice.instituteId = instituteId;
    invoice.feeAccountId = feeAccountId;
    invoice.invoiceNo = invoiceNo;
    invoice.feeCategoryId = feeCategoryId;
    invoice.financialYearId = financialYearId;
    invoice.description = description;
    invoice.amount = amount;
    invoice.amountPaid = BigDecimal.ZERO.setScale(2);
    invoice.dueDate = dueDate;
    invoice.status = STATUS_DUE;
    invoice.installmentNo = installmentNo;
    Instant now = Instant.now();
    invoice.createdAt = now;
    invoice.updatedAt = now;
    return invoice;
  }

  public BigDecimal remaining() {
    return amount.subtract(amountPaid);
  }

  public void applyPayment(BigDecimal allocated) {
    this.amountPaid = this.amountPaid.add(allocated);
    if (this.amountPaid.compareTo(this.amount) >= 0) {
      this.amountPaid = this.amount;
      this.status = STATUS_PAID;
    } else if (this.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
      this.status = STATUS_PARTIAL;
    } else {
      this.status = STATUS_DUE;
    }
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

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public Long getFeeCategoryId() {
    return feeCategoryId;
  }

  public Long getFinancialYearId() {
    return financialYearId;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getAmountPaid() {
    return amountPaid;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getStatus() {
    return status;
  }

  public Integer getInstallmentNo() {
    return installmentNo;
  }
}
