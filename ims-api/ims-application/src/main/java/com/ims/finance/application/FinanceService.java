package com.ims.finance.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.AcademicCatalogService.FeePlanView;
import com.ims.academic.application.AcademicCatalogService.InstallmentView;
import com.ims.academic.domain.Enrollment;
import com.ims.common.money.Money;
import com.ims.common.tenancy.TenantContext;
import com.ims.finance.api.FinanceDtos.FeeAccountResponse;
import com.ims.finance.api.FinanceDtos.InvoiceResponse;
import com.ims.finance.api.FinanceDtos.PaymentResponse;
import com.ims.finance.api.FinanceDtos.PaymentResultResponse;
import com.ims.finance.api.FinanceDtos.ReceiptResponse;
import com.ims.finance.domain.FinancialYear;
import com.ims.finance.domain.Invoice;
import com.ims.finance.domain.Payment;
import com.ims.finance.domain.PaymentAllocation;
import com.ims.finance.domain.Receipt;
import com.ims.finance.domain.StudentFeeAccount;
import com.ims.finance.infrastructure.FinancialYearJpaRepository;
import com.ims.finance.infrastructure.InvoiceJpaRepository;
import com.ims.finance.infrastructure.PaymentAllocationJpaRepository;
import com.ims.finance.infrastructure.PaymentJpaRepository;
import com.ims.finance.infrastructure.ReceiptJpaRepository;
import com.ims.finance.infrastructure.StudentFeeAccountJpaRepository;
import com.ims.platform.audit.application.AuditService;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
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
  private final AcademicCatalogService academicCatalogService;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;

  public FinanceService(
      StudentFeeAccountJpaRepository studentFeeAccountJpaRepository,
      InvoiceJpaRepository invoiceJpaRepository,
      PaymentJpaRepository paymentJpaRepository,
      PaymentAllocationJpaRepository paymentAllocationJpaRepository,
      ReceiptJpaRepository receiptJpaRepository,
      FinancialYearJpaRepository financialYearJpaRepository,
      AcademicCatalogService academicCatalogService,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService) {
    this.studentFeeAccountJpaRepository = studentFeeAccountJpaRepository;
    this.invoiceJpaRepository = invoiceJpaRepository;
    this.paymentJpaRepository = paymentJpaRepository;
    this.paymentAllocationJpaRepository = paymentAllocationJpaRepository;
    this.receiptJpaRepository = receiptJpaRepository;
    this.financialYearJpaRepository = financialYearJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
  }

  public record CollectPaymentCommand(
      @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank @Size(max = 32) String method) {}

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

    account.reduceOutstanding(amount);
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
    return new PaymentResultResponse(toPayment(payment), toReceipt(receipt), toFeeAccount(account));
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
        payment.getIdempotencyKey());
  }

  private ReceiptResponse toReceipt(Receipt receipt) {
    return new ReceiptResponse(
        receipt.getId(), receipt.getPaymentId(), receipt.getReceiptNo(), receipt.getIssuedAt());
  }
}
