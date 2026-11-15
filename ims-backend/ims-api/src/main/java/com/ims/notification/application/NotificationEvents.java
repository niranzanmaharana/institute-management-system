package com.ims.notification.application;

import java.math.BigDecimal;

public final class NotificationEvents {

  private NotificationEvents() {}

  public record AdmissionApproved(
      Long instituteId,
      Long admissionId,
      String applicationNo,
      String applicantName,
      String email,
      String instituteName) {}

  public record PaymentReceived(
      Long instituteId,
      Long paymentId,
      Long feeAccountId,
      String studentName,
      String email,
      String paymentNo,
      String receiptNo,
      BigDecimal amount,
      String currency,
      String instituteName) {}
}
