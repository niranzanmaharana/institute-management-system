package com.ims.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FinanceDtos {

  private FinanceDtos() {}

  public record FeeAccountResponse(
      Long id,
      Long studentId,
      Long enrollmentId,
      String currency,
      String status,
      BigDecimal outstandingAmount,
      Long version) {}

  public record InvoiceResponse(
      Long id,
      Long feeAccountId,
      String invoiceNo,
      Long feeCategoryId,
      Long financialYearId,
      String description,
      BigDecimal amount,
      BigDecimal amountPaid,
      LocalDate dueDate,
      String status,
      Integer installmentNo) {}

  public record PaymentResponse(
      Long id,
      Long feeAccountId,
      String paymentNo,
      BigDecimal amount,
      String method,
      Instant paidAt,
      String status,
      String idempotencyKey,
      String reverseReason,
      Instant reversedAt) {}

  public record ReceiptResponse(
      Long id, Long paymentId, String receiptNo, Instant issuedAt, String status) {}

  public record PaymentResultResponse(
      PaymentResponse payment, ReceiptResponse receipt, FeeAccountResponse feeAccount) {}

  public record AdjustmentResponse(
      Long id,
      Long feeAccountId,
      String type,
      BigDecimal amount,
      String reason,
      Long approvedBy,
      Instant createdAt) {}

  public record AdjustmentResultResponse(AdjustmentResponse adjustment, FeeAccountResponse feeAccount) {}

  public record RefundResponse(
      Long id,
      Long feeAccountId,
      BigDecimal amount,
      String status,
      String reason,
      Long requestedBy,
      Long decidedBy,
      Instant processedAt,
      String processNote,
      Instant createdAt) {}

  public record RefundResultResponse(RefundResponse refund, FeeAccountResponse feeAccount) {}

  public record ActivationResultResponse(
      Long enrollmentId,
      String enrollmentStatus,
      Instant activatedAt,
      FeeAccountResponse feeAccount,
      List<InvoiceResponse> invoices) {}
}
