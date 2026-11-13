package com.ims.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FeeStatementDtos {

  private FeeStatementDtos() {}

  public record FeeStatementResponse(
      Instant generatedAt,
      FeeAccountSummary feeAccount,
      StudentSummary student,
      Totals totals,
      List<InvoiceLine> invoices,
      List<PaymentLine> payments,
      List<AdjustmentLine> adjustments,
      List<RefundLine> refunds) {}

  public record FeeAccountSummary(
      Long id,
      Long studentId,
      Long enrollmentId,
      String currency,
      String status,
      BigDecimal outstandingAmount) {}

  public record StudentSummary(Long id, String studentCode, String name, String phone, String email) {}

  public record Totals(
      BigDecimal invoiceTotal,
      BigDecimal paidTotal,
      BigDecimal creditAdjustments,
      BigDecimal penalties,
      BigDecimal completedRefunds,
      BigDecimal outstandingAmount) {}

  public record InvoiceLine(
      Long id,
      String invoiceNo,
      String description,
      LocalDate dueDate,
      BigDecimal amount,
      BigDecimal amountPaid,
      BigDecimal remaining,
      String status) {}

  public record PaymentLine(
      Long id,
      String paymentNo,
      Instant paidAt,
      String method,
      BigDecimal amount,
      String status,
      String receiptNo) {}

  public record AdjustmentLine(
      Long id, Instant createdAt, String type, BigDecimal amount, String reason) {}

  public record RefundLine(
      Long id, Instant createdAt, BigDecimal amount, String status, String reason) {}
}
