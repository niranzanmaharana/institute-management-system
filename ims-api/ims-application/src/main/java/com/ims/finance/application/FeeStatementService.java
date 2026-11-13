package com.ims.finance.application;

import com.ims.common.money.Money;
import com.ims.common.tenancy.TenantContext;
import com.ims.finance.api.FeeStatementDtos.AdjustmentLine;
import com.ims.finance.api.FeeStatementDtos.FeeAccountSummary;
import com.ims.finance.api.FeeStatementDtos.FeeStatementResponse;
import com.ims.finance.api.FeeStatementDtos.InvoiceLine;
import com.ims.finance.api.FeeStatementDtos.PaymentLine;
import com.ims.finance.api.FeeStatementDtos.RefundLine;
import com.ims.finance.api.FeeStatementDtos.StudentSummary;
import com.ims.finance.api.FeeStatementDtos.Totals;
import com.ims.finance.domain.FeeAdjustment;
import com.ims.finance.domain.Invoice;
import com.ims.finance.domain.Payment;
import com.ims.finance.domain.Receipt;
import com.ims.finance.domain.RefundRequest;
import com.ims.finance.domain.StudentFeeAccount;
import com.ims.finance.infrastructure.FeeAdjustmentJpaRepository;
import com.ims.finance.infrastructure.InvoiceJpaRepository;
import com.ims.finance.infrastructure.PaymentJpaRepository;
import com.ims.finance.infrastructure.ReceiptJpaRepository;
import com.ims.finance.infrastructure.RefundRequestJpaRepository;
import com.ims.finance.infrastructure.StudentFeeAccountJpaRepository;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeeStatementService {

  private final StudentFeeAccountJpaRepository studentFeeAccountJpaRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final PaymentJpaRepository paymentJpaRepository;
  private final ReceiptJpaRepository receiptJpaRepository;
  private final FeeAdjustmentJpaRepository feeAdjustmentJpaRepository;
  private final RefundRequestJpaRepository refundRequestJpaRepository;
  private final StudentService studentService;
  private final UserAccountRepository userAccountRepository;
  private final FeeStatementPdfRenderer pdfRenderer;
  private final TenantFilterEnabler tenantFilterEnabler;

  public FeeStatementService(
      StudentFeeAccountJpaRepository studentFeeAccountJpaRepository,
      InvoiceJpaRepository invoiceJpaRepository,
      PaymentJpaRepository paymentJpaRepository,
      ReceiptJpaRepository receiptJpaRepository,
      FeeAdjustmentJpaRepository feeAdjustmentJpaRepository,
      RefundRequestJpaRepository refundRequestJpaRepository,
      StudentService studentService,
      UserAccountRepository userAccountRepository,
      FeeStatementPdfRenderer pdfRenderer,
      TenantFilterEnabler tenantFilterEnabler) {
    this.studentFeeAccountJpaRepository = studentFeeAccountJpaRepository;
    this.invoiceJpaRepository = invoiceJpaRepository;
    this.paymentJpaRepository = paymentJpaRepository;
    this.receiptJpaRepository = receiptJpaRepository;
    this.feeAdjustmentJpaRepository = feeAdjustmentJpaRepository;
    this.refundRequestJpaRepository = refundRequestJpaRepository;
    this.studentService = studentService;
    this.userAccountRepository = userAccountRepository;
    this.pdfRenderer = pdfRenderer;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  @Transactional(readOnly = true)
  public FeeStatementResponse getStatement(Long feeAccountId) {
    StudentFeeAccount account = requireAccount(feeAccountId);
    assertCanView(account);
    return build(account);
  }

  @Transactional(readOnly = true)
  public byte[] getStatementPdf(Long feeAccountId) {
    return pdfRenderer.render(getStatement(feeAccountId));
  }

  private FeeStatementResponse build(StudentFeeAccount account) {
    long instituteId = account.getInstituteId();
    StudentResponse student = studentService.get(account.getStudentId());
    List<Invoice> invoices =
        invoiceJpaRepository.findByFeeAccountIdAndInstituteIdOrderByDueDateAscIdAsc(
            account.getId(), instituteId);
    List<Payment> payments =
        paymentJpaRepository.findByFeeAccountIdAndInstituteIdOrderByPaidAtDesc(
            account.getId(), instituteId);
    List<FeeAdjustment> adjustments =
        feeAdjustmentJpaRepository.findByFeeAccountIdAndInstituteIdOrderByCreatedAtDesc(
            account.getId(), instituteId);
    List<RefundRequest> refunds =
        refundRequestJpaRepository.findByInstituteIdAndFeeAccountIdOrderByCreatedAtDesc(
            instituteId, account.getId());

    BigDecimal invoiceTotal =
        invoices.stream()
            .filter(i -> !"CANCELLED".equals(i.getStatus()) && !"WAIVED".equals(i.getStatus()))
            .map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal paidTotal =
        payments.stream()
            .filter(p -> Payment.STATUS_SUCCESS.equals(p.getStatus()))
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal credits = BigDecimal.ZERO;
    BigDecimal penalties = BigDecimal.ZERO;
    for (FeeAdjustment row : adjustments) {
      if (FeeAdjustment.TYPE_PENALTY.equals(row.getType())) {
        penalties = penalties.add(row.getAmount());
      } else if (FeeAdjustment.isCredit(row.getType())) {
        credits = credits.add(row.getAmount());
      }
    }
    BigDecimal completedRefunds =
        refunds.stream()
            .filter(r -> RefundRequest.STATUS_COMPLETED.equals(r.getStatus()))
            .map(RefundRequest::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<InvoiceLine> invoiceLines =
        invoices.stream()
            .map(
                i ->
                    new InvoiceLine(
                        i.getId(),
                        i.getInvoiceNo(),
                        i.getDescription(),
                        i.getDueDate(),
                        i.getAmount(),
                        i.getAmountPaid(),
                        Money.normalize(i.remaining()),
                        i.getStatus()))
            .toList();
    List<PaymentLine> paymentLines =
        payments.stream()
            .map(
                p -> {
                  String receiptNo =
                      receiptJpaRepository
                          .findByInstituteIdAndPaymentId(instituteId, p.getId())
                          .map(Receipt::getReceiptNo)
                          .orElse(null);
                  return new PaymentLine(
                      p.getId(),
                      p.getPaymentNo(),
                      p.getPaidAt(),
                      p.getMethod(),
                      p.getAmount(),
                      p.getStatus(),
                      receiptNo);
                })
            .toList();
    List<AdjustmentLine> adjustmentLines =
        adjustments.stream()
            .map(
                a ->
                    new AdjustmentLine(
                        a.getId(), a.getCreatedAt(), a.getType(), a.getAmount(), a.getReason()))
            .toList();
    List<RefundLine> refundLines =
        refunds.stream()
            .map(
                r ->
                    new RefundLine(
                        r.getId(), r.getCreatedAt(), r.getAmount(), r.getStatus(), r.getReason()))
            .toList();

    return new FeeStatementResponse(
        Instant.now(),
        new FeeAccountSummary(
            account.getId(),
            account.getStudentId(),
            account.getEnrollmentId(),
            account.getCurrency(),
            account.getStatus(),
            account.getOutstandingAmount()),
        new StudentSummary(
            student.id(),
            student.studentCode(),
            (student.firstName() + " " + student.lastName()).trim(),
            student.phone(),
            student.email()),
        new Totals(
            Money.normalize(invoiceTotal),
            Money.normalize(paidTotal),
            Money.normalize(credits),
            Money.normalize(penalties),
            Money.normalize(completedRefunds),
            account.getOutstandingAmount()),
        invoiceLines,
        paymentLines,
        adjustmentLines,
        refundLines);
  }

  private void assertCanView(StudentFeeAccount account) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view statement");
    }
    boolean feeRead =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("fee:read"::equals);
    if (feeRead) {
      return;
    }
    if (!(authentication.getPrincipal() instanceof ImsUserPrincipal principal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view statement");
    }
    UserAccount user =
        userAccountRepository
            .findById(principal.getUserId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view statement"));
    if (!"STUDENT".equalsIgnoreCase(user.getPersonType())
        || user.getPersonId() == null
        || !user.getPersonId().equals(account.getStudentId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view statement");
    }
  }

  private StudentFeeAccount requireAccount(Long id) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return studentFeeAccountJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee account not found"));
  }
}
