package com.ims.notification.application;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

  private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

  private final NotificationService notificationService;

  public NotificationEventListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onAdmissionApproved(NotificationEvents.AdmissionApproved event) {
    try {
      Map<String, String> vars = new LinkedHashMap<>();
      vars.put("applicationNo", nullToEmpty(event.applicationNo()));
      vars.put("applicantName", nullToEmpty(event.applicantName()));
      vars.put("instituteName", nullToEmpty(event.instituteName()));
      notificationService.enqueue(
          event.instituteId(),
          NotificationTrigger.ADMISSION_APPROVED,
          NotificationTrigger.ADMISSION_APPROVED,
          event.email(),
          vars,
          "AdmissionApplication",
          event.admissionId());
    } catch (Exception ex) {
      log.error(
          "Failed to enqueue ADMISSION_APPROVED notification admissionId={}: {}",
          event.admissionId(),
          ex.getMessage());
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onPaymentReceived(NotificationEvents.PaymentReceived event) {
    try {
      Map<String, String> vars = new LinkedHashMap<>();
      vars.put("studentName", nullToEmpty(event.studentName()));
      vars.put(
          "amount", event.amount() == null ? "" : event.amount().toPlainString());
      vars.put("currency", nullToEmpty(event.currency()));
      vars.put("receiptNo", nullToEmpty(event.receiptNo()));
      vars.put("paymentNo", nullToEmpty(event.paymentNo()));
      vars.put("instituteName", nullToEmpty(event.instituteName()));
      notificationService.enqueue(
          event.instituteId(),
          NotificationTrigger.PAYMENT_RECEIVED,
          NotificationTrigger.PAYMENT_RECEIVED,
          event.email(),
          vars,
          "Payment",
          event.paymentId());
    } catch (Exception ex) {
      log.error(
          "Failed to enqueue PAYMENT_RECEIVED notification paymentId={}: {}",
          event.paymentId(),
          ex.getMessage());
    }
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
