package com.ims.finance.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.AcademicCatalogService.FeePlanView;
import com.ims.academic.application.AcademicCatalogService.InstallmentView;
import com.ims.academic.domain.Enrollment;
import com.ims.common.money.Money;
import com.ims.common.tenancy.TenantContext;
import com.ims.finance.api.FinanceDtos.AdjustmentResponse;
import com.ims.finance.api.FinanceDtos.AdjustmentResultResponse;
import com.ims.finance.api.FinanceDtos.FeeAccountResponse;
import com.ims.finance.api.FinanceDtos.InvoiceResponse;
import com.ims.finance.api.FinanceDtos.PaymentResponse;
import com.ims.finance.api.FinanceDtos.PaymentResultResponse;
import com.ims.finance.api.FinanceDtos.ReceiptResponse;
import com.ims.finance.api.FinanceDtos.RefundResponse;
import com.ims.finance.api.FinanceDtos.RefundResultResponse;
import com.ims.finance.domain.FeeAdjustment;
import com.ims.finance.domain.FinancialYear;
import com.ims.finance.domain.Invoice;
import com.ims.finance.domain.Payment;
import com.ims.finance.domain.PaymentAllocation;
import com.ims.finance.domain.Receipt;
import com.ims.finance.domain.RefundRequest;
import com.ims.finance.domain.StudentFeeAccount;
import com.ims.finance.infrastructure.FeeAdjustmentJpaRepository;
import com.ims.finance.infrastructure.FinancialYearJpaRepository;
import com.ims.finance.infrastructure.InvoiceJpaRepository;
import com.ims.finance.infrastructure.PaymentAllocationJpaRepository;
import com.ims.finance.infrastructure.PaymentJpaRepository;
import com.ims.finance.infrastructure.RefundRequestJpaRepository;
import com.ims.finance.infrastructure.ReceiptJpaRepository;
import com.ims.finance.infrastructure.StudentFeeAccountJpaRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.notification.application.NotificationEvents;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.institute.application.InstituteService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FinanceService {

  private static final Logger log = LoggerFactory.getLogger(FinanceService.class);

  private final StudentFeeAccountJpaRepository studentFeeAccountJpaRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final PaymentJpaRepository paymentJpaRepository;
  private final PaymentAllocationJpaRepository paymentAllocationJpaRepository;
  private final ReceiptJpaRepository receiptJpaRepository;
  private final FinancialYearJpaRepository financialYearJpaRepository;
  private final FeeAdjustmentJpaRepository feeAdjustmentJpaRepository;
  private final RefundRequestJpaRepository refundRequestJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;
  private final ApplicationEventPublisher eventPublisher;
  private final StudentService studentService;
  private final InstituteService instituteService;

  public FinanceService(
      StudentFeeAccountJpaRepository studentFeeAccountJpaRepository,
      InvoiceJpaRepository invoiceJpaRepository,
      PaymentJpaRepository paymentJpaRepository,
      PaymentAllocationJpaRepository paymentAllocationJpaRepository,
      ReceiptJpaRepository receiptJpaRepository,
      FinancialYearJpaRepository financialYearJpaRepository,
      FeeAdjustmentJpaRepository feeAdjustmentJpaRepository,
      RefundRequestJpaRepository refundRequestJpaRepository,
      AcademicCatalogService academicCatalogService,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService,
      ApplicationEventPublisher eventPublisher,
      StudentService studentService,
      InstituteService instituteService) {
    this.studentFeeAccountJpaRepository = studentFeeAccountJpaRepository;
    this.invoiceJpaRepository = invoiceJpaRepository;
    this.paymentJpaRepository = paymentJpaRepository;
    this.paymentAllocationJpaRepository = paymentAllocationJpaRepository;
    this.receiptJpaRepository = receiptJpaRepository;
    this.financialYearJpaRepository = financialYearJpaRepository;
    this.feeAdjustmentJpaRepository = feeAdjustmentJpaRepository;
    this.refundRequestJpaRepository = refundRequestJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
    this.eventPublisher = eventPublisher;
    this.studentService = studentService;
    this.instituteService = instituteService;
  }

  public record CollectPaymentCommand(
      @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank @Size(max = 32) String method) {}

  public record CreateAdjustmentCommand(
      @NotBlank @Size(max = 32) String type,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotBlank @Size(min = 5, max = 512) String reason) {}

  public record CreateRefundCommand(
      @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank @Size(min = 5, max = 512) String reason) {}

  public record RejectRefundCommand(@NotBlank @Size(min = 5, max = 512) String reason) {}

  public record ProcessRefundCommand(boolean success, @Size(max = 512) String note) {}

  public record ReversePaymentCommand(@NotBlank @Size(min = 5, max = 512) String reason) {}

  @Transactional
  public FeeAccountResponse ensureFeeAccountWithInvoices(Enrollment enrollment) {
    long instituteId = requireTenant();
    return studentFeeAccountJpaRepository
        .findByInstituteIdAndEnrollmentId(instituteId, enrollment.getId())
        .map(this::toFeeAccount)
        .orElseGet(() -> createFeeAccountWithInvoices(enrollment));
  }

  @Transactional(readOnly = true)
  public FeeAccountResponse getFeeAccountForEnrollment(Long enrollmentId) {
    long instituteId = requireTenant();
    StudentFeeAccount account =
        studentFeeAccountJpaRepository
            .findByInstituteIdAndEnrollmentId(instituteId, enrollmentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee account not found"));
    return toFeeAccount(account);
  }

  @Transactional(readOnly = true)
  public List<InvoiceResponse> listInvoicesForEnrollment(Long enrollmentId) {
    long instituteId = requireTenant();
    StudentFeeAccount account =
        studentFeeAccountJpaRepository
            .findByInstituteIdAndEnrollmentId(instituteId, enrollmentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee account not found"));
    return listInvoices(account.getId());
  }

  @Transactional(readOnly = true)
  public FeeAccountResponse getFeeAccount(Long id) {
    return toFeeAccount(requireAccount(id));
  }

  @Transactional(readOnly = true)
  public List<FeeAccountResponse> listFeeAccountsByStudent(Long studentId) {
    long instituteId = requireTenant();
    return studentFeeAccountJpaRepository.findByInstituteIdAndStudentId(instituteId, studentId)
        .stream()
        .map(this::toFeeAccount)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InvoiceResponse> listInvoices(Long feeAccountId) {
    long instituteId = requireTenant();
    requireAccount(feeAccountId);
    return invoiceJpaRepository
        .findByFeeAccountIdAndInstituteIdOrderByDueDateAscIdAsc(feeAccountId, instituteId)
        .stream()
        .map(this::toInvoice)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FeeAccountResponse> listOutstanding() {
    long instituteId = requireTenant();
    return studentFeeAccountJpaRepository
        .findByInstituteIdAndStatusAndOutstandingAmountGreaterThan(
            instituteId, StudentFeeAccount.STATUS_OPEN, BigDecimal.ZERO)
        .stream()
        .map(this::toFeeAccount)
        .toList();
  }

  @Transactional
  public PaymentResultResponse collectPayment(
      Long feeAccountId, CollectPaymentCommand command, String idempotencyKey) {
    long instituteId = requireTenant();
    BigDecimal amount = Money.normalize(command.amount());
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
    }

    String key =
        idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    if (key != null) {
      var existing = paymentJpaRepository.findByInstituteIdAndIdempotencyKey(instituteId, key);
      if (existing.isPresent()) {
        Payment payment = existing.get();
        Receipt receipt =
            receiptJpaRepository
                .findByInstituteIdAndPaymentId(instituteId, payment.getId())
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.CONFLICT, "Idempotent payment missing receipt"));
        StudentFeeAccount account = requireAccount(payment.getFeeAccountId());
        log.info(
            "Payment idempotent hit paymentId={} key={} instituteId={}",
            payment.getId(),
            key,
            instituteId);
        return new PaymentResultResponse(toPayment(payment), toReceipt(receipt), toFeeAccount(account));
      }
    }

    StudentFeeAccount account = requireAccount(feeAccountId);
    if (amount.compareTo(account.getOutstandingAmount()) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "amount exceeds outstanding balance");
    }

    Payment payment =
        paymentJpaRepository.save(
            Payment.create(
                instituteId,
                account.getId(),
                "TMP-" + System.currentTimeMillis(),
                amount,
                command.method(),
                key));
    payment.assignPaymentNo("PAY-" + payment.getId());
    paymentJpaRepository.save(payment);

    BigDecimal remaining = amount;
    List<Invoice> openInvoices =
        invoiceJpaRepository.findByFeeAccountIdAndInstituteIdAndStatusInOrderByDueDateAscIdAsc(
            account.getId(),
            instituteId,
            List.of(Invoice.STATUS_DUE, Invoice.STATUS_PARTIAL));
    for (Invoice invoice : openInvoices) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal due = Money.normalize(invoice.remaining());
      if (due.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal allocated = due.min(remaining);
      invoice.applyPayment(allocated);
      invoiceJpaRepository.save(invoice);
      paymentAllocationJpaRepository.save(
          PaymentAllocation.create(instituteId, payment.getId(), invoice.getId(), allocated));
      remaining = remaining.subtract(allocated);
    }

    refreshOutstanding(account);
    try {
      studentFeeAccountJpaRepository.saveAndFlush(account);
    } catch (OptimisticLockingFailureException ex) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Fee account was updated concurrently; retry payment");
    }

    Receipt receipt =
        receiptJpaRepository.save(
            Receipt.create(instituteId, payment.getId(), "RCP-" + payment.getId()));
    log.info(
        "Payment collected paymentId={} accountId={} amount={} instituteId={}",
        payment.getId(),
        account.getId(),
        amount,
        instituteId);
    auditService.record(
        instituteId,
        "finance",
        "Payment",
        payment.getId(),
        "PAYMENT_SUCCESS",
        null,
        java.util.Map.of(
            "feeAccountId",
            account.getId(),
            "amount",
            amount,
            "receiptNo",
            receipt.getReceiptNo()),
        null);
    StudentResponse student = studentService.get(account.getStudentId());
    String studentName =
        ((student.firstName() == null ? "" : student.firstName())
                + " "
                + (student.lastName() == null ? "" : student.lastName()))
            .trim();
    String instituteName =
        instituteService.listVisible(instituteId).stream()
            .findFirst()
            .map(i -> i.name())
            .orElse("Institute");
    eventPublisher.publishEvent(
        new NotificationEvents.PaymentReceived(
            instituteId,
            payment.getId(),
            account.getId(),
            studentName.isBlank() ? "Student" : studentName,
            student.email(),
            payment.getPaymentNo(),
            receipt.getReceiptNo(),
            amount,
            account.getCurrency() == null ? "INR" : account.getCurrency(),
            instituteName));
    return new PaymentResultResponse(toPayment(payment), toReceipt(receipt), toFeeAccount(account));
  }

  @Transactional(readOnly = true)
  public List<AdjustmentResponse> listAdjustments(Long feeAccountId) {
    long instituteId = requireTenant();
    requireAccount(feeAccountId);
    return feeAdjustmentJpaRepository
        .findByFeeAccountIdAndInstituteIdOrderByCreatedAtDesc(feeAccountId, instituteId)
        .stream()
        .map(this::toAdjustment)
        .toList();
  }

  @Transactional
  public AdjustmentResultResponse createAdjustment(Long feeAccountId, CreateAdjustmentCommand command) {
    long instituteId = requireTenant();
    String type;
    try {
      type = FeeAdjustment.normalizeType(command.type());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    BigDecimal amount = Money.normalize(command.amount());
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
    }
    StudentFeeAccount account = requireAccount(feeAccountId);
    if (FeeAdjustment.isCredit(type) && amount.compareTo(account.getOutstandingAmount()) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "adjustment exceeds outstanding balance");
    }
    FeeAdjustment saved =
        feeAdjustmentJpaRepository.save(
            FeeAdjustment.create(
                instituteId, feeAccountId, type, amount, command.reason(), currentUserId()));
    refreshOutstanding(account);
    if (account.getOutstandingAmount().compareTo(BigDecimal.ZERO) < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "adjustment would make outstanding negative");
    }
    try {
      studentFeeAccountJpaRepository.saveAndFlush(account);
    } catch (OptimisticLockingFailureException ex) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Fee account was updated concurrently; retry adjustment");
    }
    auditService.record(
        instituteId,
        "finance",
        "FeeAdjustment",
        saved.getId(),
        "FEE_ADJUSTED",
        null,
        java.util.Map.of(
            "feeAccountId",
            feeAccountId,
            "type",
            type,
            "amount",
            amount,
            "outstandingAmount",
            account.getOutstandingAmount()),
        command.reason());
    log.info(
        "Fee adjustment id={} type={} amount={} accountId={} instituteId={}",
        saved.getId(),
        type,
        amount,
        feeAccountId,
        instituteId);
    return new AdjustmentResultResponse(toAdjustment(saved), toFeeAccount(account));
  }

  @Transactional(readOnly = true)
  public List<RefundResponse> listRefunds(Long feeAccountId, String status) {
    long instituteId = requireTenant();
    List<RefundRequest> rows;
    if (feeAccountId != null) {
      requireAccount(feeAccountId);
      rows =
          refundRequestJpaRepository.findByInstituteIdAndFeeAccountIdOrderByCreatedAtDesc(
              instituteId, feeAccountId);
    } else if (status != null && !status.isBlank()) {
      rows =
          refundRequestJpaRepository.findByInstituteIdAndStatusOrderByCreatedAtDesc(
              instituteId, status.trim().toUpperCase());
    } else {
      rows = refundRequestJpaRepository.findByInstituteIdOrderByCreatedAtDesc(instituteId);
    }
    return rows.stream().map(this::toRefund).toList();
  }

  @Transactional
  public RefundResultResponse requestRefund(Long feeAccountId, CreateRefundCommand command) {
    long instituteId = requireTenant();
    StudentFeeAccount account = requireAccount(feeAccountId);
    BigDecimal amount = Money.normalize(command.amount());
    BigDecimal available = refundableBalance(instituteId, feeAccountId);
    if (amount.compareTo(available) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "refund exceeds net successful payments");
    }
    RefundRequest saved =
        refundRequestJpaRepository.save(
            RefundRequest.create(
                instituteId, feeAccountId, amount, command.reason(), currentUserId()));
    auditService.record(
        instituteId,
        "finance",
        "RefundRequest",
        saved.getId(),
        "REFUND_REQUESTED",
        null,
        java.util.Map.of("feeAccountId", feeAccountId, "amount", amount),
        command.reason());
    return new RefundResultResponse(toRefund(saved), toFeeAccount(account));
  }

  @Transactional
  public RefundResponse approveRefund(Long refundId) {
    RefundRequest row = requireRefund(refundId);
    try {
      row.approve(currentUserId());
    } catch (IllegalStateException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
    }
    refundRequestJpaRepository.save(row);
    auditService.record(
        row.getInstituteId(),
        "finance",
        "RefundRequest",
        row.getId(),
        "REFUND_APPROVED",
        RefundRequest.STATUS_REQUESTED,
        row.getStatus(),
        null);
    return toRefund(row);
  }

  @Transactional
  public RefundResponse rejectRefund(Long refundId, RejectRefundCommand command) {
    RefundRequest row = requireRefund(refundId);
    try {
      row.reject(currentUserId(), command.reason());
    } catch (IllegalStateException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
    }
    refundRequestJpaRepository.save(row);
    auditService.record(
        row.getInstituteId(),
        "finance",
        "RefundRequest",
        row.getId(),
        "REFUND_REJECTED",
        RefundRequest.STATUS_REQUESTED,
        row.getStatus(),
        command.reason());
    return toRefund(row);
  }

  @Transactional
  public RefundResultResponse processRefund(
      Long refundId, ProcessRefundCommand command, String idempotencyKey) {
    long instituteId = requireTenant();
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key is required");
    }
    String key = idempotencyKey.trim();
    var existing = refundRequestJpaRepository.findByInstituteIdAndProcessIdempotencyKey(instituteId, key);
    if (existing.isPresent()) {
      RefundRequest prior = existing.get();
      if (!prior.getId().equals(refundId)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key already used");
      }
      StudentFeeAccount account = requireAccount(prior.getFeeAccountId());
      return new RefundResultResponse(toRefund(prior), toFeeAccount(account));
    }
    RefundRequest row = requireRefund(refundId);
    try {
      row.markProcessing(key);
    } catch (IllegalStateException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
    }
    refundRequestJpaRepository.save(row);
    if (command.success()) {
      row.complete(blankToNull(command.note()));
      refundRequestJpaRepository.save(row);
      StudentFeeAccount account = requireAccount(row.getFeeAccountId());
      refreshOutstanding(account);
      try {
        studentFeeAccountJpaRepository.saveAndFlush(account);
      } catch (OptimisticLockingFailureException ex) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Fee account was updated concurrently; retry refund");
      }
      auditService.record(
          instituteId,
          "finance",
          "RefundRequest",
          row.getId(),
          "REFUND_COMPLETED",
          RefundRequest.STATUS_APPROVED,
          row.getStatus(),
          command.note());
      return new RefundResultResponse(toRefund(row), toFeeAccount(account));
    }
    row.fail(blankToNull(command.note()));
    refundRequestJpaRepository.save(row);
    StudentFeeAccount account = requireAccount(row.getFeeAccountId());
    auditService.record(
        instituteId,
        "finance",
        "RefundRequest",
        row.getId(),
        "REFUND_FAILED",
        RefundRequest.STATUS_APPROVED,
        row.getStatus(),
        command.note());
    return new RefundResultResponse(toRefund(row), toFeeAccount(account));
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> listPayments(Long feeAccountId) {
    long instituteId = requireTenant();
    requireAccount(feeAccountId);
    return paymentJpaRepository
        .findByFeeAccountIdAndInstituteIdOrderByPaidAtDesc(feeAccountId, instituteId)
        .stream()
        .map(this::toPayment)
        .toList();
  }

  @Transactional
  public PaymentResultResponse reversePayment(Long paymentId, ReversePaymentCommand command) {
    long instituteId = requireTenant();
    Payment payment =
        paymentJpaRepository
            .findByIdAndInstituteId(paymentId, instituteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    if (!Payment.STATUS_SUCCESS.equals(payment.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUCCESS payments can be reversed");
    }
    List<PaymentAllocation> allocations =
        paymentAllocationJpaRepository.findByPaymentIdAndInstituteId(paymentId, instituteId);
    for (PaymentAllocation allocation : allocations) {
      Invoice invoice =
          invoiceJpaRepository
              .findByIdAndInstituteId(allocation.getInvoiceId(), instituteId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
      invoice.reversePayment(Money.normalize(allocation.getAmount()));
      invoiceJpaRepository.save(invoice);
    }
    paymentAllocationJpaRepository.deleteAll(allocations);
    try {
      payment.reverse(command.reason(), currentUserId());
    } catch (IllegalStateException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
    }
    paymentJpaRepository.save(payment);
    Receipt receipt =
        receiptJpaRepository
            .findByInstituteIdAndPaymentId(instituteId, paymentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
    receipt.cancel();
    receiptJpaRepository.save(receipt);
    StudentFeeAccount account = requireAccount(payment.getFeeAccountId());
    refreshOutstanding(account);
    try {
      studentFeeAccountJpaRepository.saveAndFlush(account);
    } catch (OptimisticLockingFailureException ex) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Fee account was updated concurrently; retry reversal");
    }
    auditService.record(
        instituteId,
        "finance",
        "Payment",
        payment.getId(),
        "PAYMENT_REVERSED",
        Payment.STATUS_SUCCESS,
        java.util.Map.of(
            "status",
            payment.getStatus(),
            "amount",
            payment.getAmount(),
            "feeAccountId",
            account.getId(),
            "outstandingAmount",
            account.getOutstandingAmount()),
        command.reason());
    log.info(
        "Payment reversed paymentId={} accountId={} amount={} instituteId={}",
        payment.getId(),
        account.getId(),
        payment.getAmount(),
        instituteId);
    return new PaymentResultResponse(toPayment(payment), toReceipt(receipt), toFeeAccount(account));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private FeeAccountResponse createFeeAccountWithInvoices(Enrollment enrollment) {
    long instituteId = enrollment.getInstituteId();
    FeePlanView feePlan = academicCatalogService.getFeePlan(enrollment.getFeePlanId());
    FinancialYear financialYear = ensureActiveFinancialYear(instituteId);

    StudentFeeAccount account =
        studentFeeAccountJpaRepository.save(
            StudentFeeAccount.create(
                instituteId,
                enrollment.getStudentId(),
                enrollment.getId(),
                feePlan.currency()));

    LocalDate today = LocalDate.now();
    BigDecimal outstanding = BigDecimal.ZERO.setScale(2);
    List<Invoice> invoices = new ArrayList<>();
    for (InstallmentView installment : feePlan.installments()) {
      BigDecimal snap = Money.normalize(installment.amount());
      Invoice invoice =
          Invoice.create(
              instituteId,
              account.getId(),
              "INV-" + account.getId() + "-" + installment.seq(),
              installment.feeCategoryId(),
              financialYear.getId(),
              installment.label(),
              snap,
              today.plusDays(installment.dueOffsetDays()),
              installment.seq());
      invoices.add(invoice);
      outstanding = outstanding.add(snap);
    }
    invoiceJpaRepository.saveAll(invoices);
    account.setOutstandingAmount(Money.normalize(outstanding));
    studentFeeAccountJpaRepository.save(account);
    log.info(
        "Fee account created id={} enrollmentId={} outstanding={} instituteId={}",
        account.getId(),
        enrollment.getId(),
        account.getOutstandingAmount(),
        instituteId);
    return toFeeAccount(account);
  }

  private FinancialYear ensureActiveFinancialYear(long instituteId) {
    return financialYearJpaRepository
        .findFirstByInstituteIdAndActiveTrue(instituteId)
        .orElseGet(
            () -> {
              int year = LocalDate.now().getYear();
              FinancialYear created =
                  financialYearJpaRepository.save(
                      FinancialYear.create(
                          instituteId,
                          "FY-" + year,
                          LocalDate.of(year, 1, 1),
                          LocalDate.of(year, 12, 31),
                          true));
              log.info(
                  "Default financial year created id={} name={} instituteId={}",
                  created.getId(),
                  created.getName(),
                  instituteId);
              return created;
            });
  }

  private void refreshOutstanding(StudentFeeAccount account) {
    account.setOutstandingAmount(computeOutstanding(account.getInstituteId(), account.getId()));
  }

  private BigDecimal computeOutstanding(long instituteId, Long feeAccountId) {
    BigDecimal invoiceTotal =
        invoiceJpaRepository
            .findByFeeAccountIdAndInstituteIdOrderByDueDateAscIdAsc(feeAccountId, instituteId)
            .stream()
            .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"WAIVED".equals(i.getStatus()))
            .map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal paid =
        paymentJpaRepository
            .findByFeeAccountIdAndInstituteIdAndStatus(
                feeAccountId, instituteId, Payment.STATUS_SUCCESS)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal credits = BigDecimal.ZERO;
    BigDecimal penalties = BigDecimal.ZERO;
    for (FeeAdjustment row :
        feeAdjustmentJpaRepository.findByFeeAccountIdAndInstituteIdOrderByCreatedAtDesc(
            feeAccountId, instituteId)) {
      if (FeeAdjustment.TYPE_PENALTY.equals(row.getType())) {
        penalties = penalties.add(row.getAmount());
      } else if (FeeAdjustment.isCredit(row.getType())) {
        credits = credits.add(row.getAmount());
      }
    }
    BigDecimal completedRefunds = completedRefunds(instituteId, feeAccountId);
    return Money.normalize(
        invoiceTotal.subtract(paid).subtract(credits).add(penalties).add(completedRefunds));
  }

  private BigDecimal refundableBalance(long instituteId, Long feeAccountId) {
    BigDecimal paid =
        paymentJpaRepository
            .findByFeeAccountIdAndInstituteIdAndStatus(
                feeAccountId, instituteId, Payment.STATUS_SUCCESS)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal reserved =
        refundRequestJpaRepository
            .findByFeeAccountIdAndInstituteIdAndStatusIn(
                feeAccountId,
                instituteId,
                List.of(
                    RefundRequest.STATUS_REQUESTED,
                    RefundRequest.STATUS_APPROVED,
                    RefundRequest.STATUS_PROCESSING,
                    RefundRequest.STATUS_COMPLETED))
            .stream()
            .map(RefundRequest::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Money.normalize(paid.subtract(reserved));
  }

  private BigDecimal completedRefunds(long instituteId, Long feeAccountId) {
    return refundRequestJpaRepository
        .findByFeeAccountIdAndInstituteIdAndStatusIn(
            feeAccountId, instituteId, List.of(RefundRequest.STATUS_COMPLETED))
        .stream()
        .map(RefundRequest::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private RefundRequest requireRefund(Long id) {
    long instituteId = requireTenant();
    return refundRequestJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund not found"));
  }

  private Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof ImsUserPrincipal principal) {
      return principal.getUserId();
    }
    return null;
  }

  private StudentFeeAccount requireAccount(Long id) {
    long instituteId = requireTenant();
    return studentFeeAccountJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee account not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private FeeAccountResponse toFeeAccount(StudentFeeAccount account) {
    return new FeeAccountResponse(
        account.getId(),
        account.getStudentId(),
        account.getEnrollmentId(),
        account.getCurrency(),
        account.getStatus(),
        account.getOutstandingAmount(),
        account.getVersion());
  }

  private InvoiceResponse toInvoice(Invoice invoice) {
    return new InvoiceResponse(
        invoice.getId(),
        invoice.getFeeAccountId(),
        invoice.getInvoiceNo(),
        invoice.getFeeCategoryId(),
        invoice.getFinancialYearId(),
        invoice.getDescription(),
        invoice.getAmount(),
        invoice.getAmountPaid(),
        invoice.getDueDate(),
        invoice.getStatus(),
        invoice.getInstallmentNo());
  }

  private PaymentResponse toPayment(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getFeeAccountId(),
        payment.getPaymentNo(),
        payment.getAmount(),
        payment.getMethod(),
        payment.getPaidAt(),
        payment.getStatus(),
        payment.getIdempotencyKey(),
        payment.getReverseReason(),
        payment.getReversedAt());
  }

  private ReceiptResponse toReceipt(Receipt receipt) {
    return new ReceiptResponse(
        receipt.getId(),
        receipt.getPaymentId(),
        receipt.getReceiptNo(),
        receipt.getIssuedAt(),
        receipt.getStatus());
  }

  private AdjustmentResponse toAdjustment(FeeAdjustment row) {
    return new AdjustmentResponse(
        row.getId(),
        row.getFeeAccountId(),
        row.getType(),
        row.getAmount(),
        row.getReason(),
        row.getApprovedBy(),
        row.getCreatedAt());
  }

  private RefundResponse toRefund(RefundRequest row) {
    return new RefundResponse(
        row.getId(),
        row.getFeeAccountId(),
        row.getAmount(),
        row.getStatus(),
        row.getReason(),
        row.getRequestedBy(),
        row.getDecidedBy(),
        row.getProcessedAt(),
        row.getProcessNote(),
        row.getCreatedAt());
  }
}
