package com.ims.finance.api;

import com.ims.finance.api.FeeStatementDtos.FeeStatementResponse;
import com.ims.finance.api.FinanceDtos.AdjustmentResponse;
import com.ims.finance.api.FinanceDtos.AdjustmentResultResponse;
import com.ims.finance.api.FinanceDtos.FeeAccountResponse;
import com.ims.finance.api.FinanceDtos.InvoiceResponse;
import com.ims.finance.api.FinanceDtos.PaymentResponse;
import com.ims.finance.api.FinanceDtos.PaymentResultResponse;
import com.ims.finance.api.FinanceDtos.RefundResponse;
import com.ims.finance.api.FinanceDtos.RefundResultResponse;
import com.ims.finance.application.FeeStatementService;
import com.ims.finance.application.FinanceService;
import com.ims.finance.application.FinanceService.CollectPaymentCommand;
import com.ims.finance.application.FinanceService.CreateAdjustmentCommand;
import com.ims.finance.application.FinanceService.CreateRefundCommand;
import com.ims.finance.application.FinanceService.ProcessRefundCommand;
import com.ims.finance.application.FinanceService.RejectRefundCommand;
import com.ims.finance.application.FinanceService.ReversePaymentCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Finance", description = "Fee accounts, invoices, payments")
@SecurityRequirement(name = "bearer-jwt")
public class FeeAccountController {

  private final FinanceService financeService;
  private final FeeStatementService feeStatementService;

  public FeeAccountController(
      FinanceService financeService, FeeStatementService feeStatementService) {
    this.financeService = financeService;
    this.feeStatementService = feeStatementService;
  }

  public record CollectPaymentRequest(
      @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank @Size(max = 32) String method) {}

  public record CreateAdjustmentRequest(
      @NotBlank @Size(max = 32) String type,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotBlank @Size(min = 5, max = 512) String reason) {}

  public record CreateRefundRequest(
      @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank @Size(min = 5, max = 512) String reason) {}

  public record RejectRefundRequest(@NotBlank @Size(min = 5, max = 512) String reason) {}

  public record ProcessRefundRequest(boolean success, @Size(max = 512) String note) {}

  public record ReversePaymentRequest(@NotBlank @Size(min = 5, max = 512) String reason) {}

  @GetMapping("/fee-accounts/{id}")
  @Operation(summary = "Get fee account")
  @PreAuthorize("hasAuthority('fee:read')")
  public FeeAccountResponse get(@PathVariable("id") Long id) {
    return financeService.getFeeAccount(id);
  }

  @GetMapping("/fee-accounts")
  @Operation(summary = "List fee accounts by student")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<FeeAccountResponse> listByStudent(@RequestParam("studentId") Long studentId) {
    return financeService.listFeeAccountsByStudent(studentId);
  }

  @GetMapping("/fee-accounts/{id}/invoices")
  @Operation(summary = "List invoices for fee account")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<InvoiceResponse> listInvoices(@PathVariable("id") Long id) {
    return financeService.listInvoices(id);
  }

  @GetMapping("/fee-accounts/{id}/statement")
  @Operation(summary = "Fee statement JSON (RP-FI-05)")
  @PreAuthorize("hasAuthority('fee:read') or hasRole('STUDENT')")
  public FeeStatementResponse statement(@PathVariable("id") Long id) {
    return feeStatementService.getStatement(id);
  }

  @GetMapping("/fee-accounts/{id}/statement.pdf")
  @Operation(summary = "Fee statement PDF download")
  @PreAuthorize("hasAuthority('fee:read') or hasRole('STUDENT')")
  public ResponseEntity<byte[]> statementPdf(@PathVariable("id") Long id) {
    byte[] pdf = feeStatementService.getStatementPdf(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"fee-statement-" + id + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  @GetMapping("/outstanding")
  @Operation(summary = "List OPEN fee accounts with outstanding > 0")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<FeeAccountResponse> outstanding() {
    return financeService.listOutstanding();
  }

  @GetMapping("/fee-accounts/{id}/payments")
  @Operation(summary = "List payments for fee account")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<PaymentResponse> listPayments(@PathVariable("id") Long id) {
    return financeService.listPayments(id);
  }

  @PostMapping("/fee-accounts/{id}/payments")
  @Operation(summary = "Collect payment against fee account")
  @PreAuthorize("hasAuthority('fee:collect')")
  public PaymentResultResponse collect(
      @PathVariable("id") Long id,
      @Valid @RequestBody CollectPaymentRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return financeService.collectPayment(
        id, new CollectPaymentCommand(request.amount(), request.method()), idempotencyKey);
  }

  @PostMapping("/payments/{id}/reverse")
  @Operation(summary = "Reverse a SUCCESS payment (BR-PV-*)")
  @PreAuthorize("hasAuthority('fee:reverse')")
  public PaymentResultResponse reversePayment(
      @PathVariable("id") Long id, @Valid @RequestBody ReversePaymentRequest request) {
    return financeService.reversePayment(id, new ReversePaymentCommand(request.reason()));
  }

  @GetMapping("/fee-accounts/{id}/adjustments")
  @Operation(summary = "List fee adjustments")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<AdjustmentResponse> listAdjustments(@PathVariable("id") Long id) {
    return financeService.listAdjustments(id);
  }

  @PostMapping("/fee-accounts/{id}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create discount, scholarship, waiver, or penalty")
  @PreAuthorize("hasAuthority('fee:adjust')")
  public AdjustmentResultResponse createAdjustment(
      @PathVariable("id") Long id, @Valid @RequestBody CreateAdjustmentRequest request) {
    return financeService.createAdjustment(
        id, new CreateAdjustmentCommand(request.type(), request.amount(), request.reason()));
  }

  @GetMapping("/refunds")
  @Operation(summary = "List refunds (RP-FI-06)")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<RefundResponse> listRefunds(
      @RequestParam(name = "feeAccountId", required = false) Long feeAccountId,
      @RequestParam(name = "status", required = false) String status) {
    return financeService.listRefunds(feeAccountId, status);
  }

  @GetMapping("/fee-accounts/{id}/refunds")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<RefundResponse> listAccountRefunds(@PathVariable("id") Long id) {
    return financeService.listRefunds(id, null);
  }

  @PostMapping("/fee-accounts/{id}/refunds")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('fee:refund')")
  public RefundResultResponse requestRefund(
      @PathVariable("id") Long id, @Valid @RequestBody CreateRefundRequest request) {
    return financeService.requestRefund(
        id, new CreateRefundCommand(request.amount(), request.reason()));
  }

  @PostMapping("/refunds/{id}/approve")
  @PreAuthorize("hasAuthority('fee:refund')")
  public RefundResponse approveRefund(@PathVariable("id") Long id) {
    return financeService.approveRefund(id);
  }

  @PostMapping("/refunds/{id}/reject")
  @PreAuthorize("hasAuthority('fee:refund')")
  public RefundResponse rejectRefund(
      @PathVariable("id") Long id, @Valid @RequestBody RejectRefundRequest request) {
    return financeService.rejectRefund(id, new RejectRefundCommand(request.reason()));
  }

  @PostMapping("/refunds/{id}/process")
  @PreAuthorize("hasAuthority('fee:refund')")
  public RefundResultResponse processRefund(
      @PathVariable("id") Long id,
      @Valid @RequestBody ProcessRefundRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return financeService.processRefund(
        id, new ProcessRefundCommand(request.success(), request.note()), idempotencyKey);
  }
}
