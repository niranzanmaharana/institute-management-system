package com.ims.finance.api;

import com.ims.finance.api.FinanceDtos.FeeAccountResponse;
import com.ims.finance.api.FinanceDtos.InvoiceResponse;
import com.ims.finance.api.FinanceDtos.PaymentResultResponse;
import com.ims.finance.application.FinanceService;
import com.ims.finance.application.FinanceService.CollectPaymentCommand;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Finance", description = "Fee accounts, invoices, payments")
@SecurityRequirement(name = "bearer-jwt")
public class FeeAccountController {

  private final FinanceService financeService;

  public FeeAccountController(FinanceService financeService) {
    this.financeService = financeService;
  }

  public record CollectPaymentRequest(
      @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank @Size(max = 32) String method) {}

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

  @GetMapping("/outstanding")
  @Operation(summary = "List OPEN fee accounts with outstanding > 0")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<FeeAccountResponse> outstanding() {
    return financeService.listOutstanding();
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
}
