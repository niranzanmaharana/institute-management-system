package com.ims.finance.api;

import com.ims.finance.application.FinanceReportService;
import com.ims.finance.application.FinanceReportService.AgingReport;
import com.ims.finance.application.FinanceReportService.DailyCollectionRow;
import com.ims.finance.application.FinanceReportService.NamedAmount;
import com.ims.finance.application.FinanceReportService.OutstandingRow;
import com.ims.finance.application.FinancialYearService;
import com.ims.finance.application.FinancialYearService.CreateFinancialYearCommand;
import com.ims.finance.application.FinancialYearService.FinancialYearView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Finance reports", description = "Aging, collections, financial years")
@SecurityRequirement(name = "bearer-jwt")
public class FinanceReportController {

  private final FinanceReportService financeReportService;
  private final FinancialYearService financialYearService;

  public FinanceReportController(
      FinanceReportService financeReportService, FinancialYearService financialYearService) {
    this.financeReportService = financeReportService;
    this.financialYearService = financialYearService;
  }

  public record CreateFinancialYearRequest(
      @NotBlank @Size(max = 64) String name,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      boolean activate) {}

  @GetMapping("/financial-years")
  @PreAuthorize("hasAuthority('fee:read')")
  @Operation(summary = "List financial years")
  public List<FinancialYearView> listYears() {
    return financialYearService.list();
  }

  @PostMapping("/financial-years")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('fee:adjust')")
  @Operation(summary = "Create financial year (ADMIN/ACCOUNTANT)")
  public FinancialYearView createYear(@Valid @RequestBody CreateFinancialYearRequest request) {
    return financialYearService.create(
        new CreateFinancialYearCommand(
            request.name(), request.startDate(), request.endDate(), request.activate()));
  }

  @PostMapping("/financial-years/{id}/activate")
  @PreAuthorize("hasAuthority('fee:adjust')")
  @Operation(summary = "Activate financial year (deactivates others)")
  public FinancialYearView activateYear(@PathVariable("id") Long id) {
    return financialYearService.activate(id);
  }

  @GetMapping("/finance/reports/aging")
  @PreAuthorize("hasAuthority('fee:read')")
  @Operation(summary = "Overdue invoice aging buckets (RP-FI-04)")
  public AgingReport aging(
      @RequestParam(name = "asOf", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return financeReportService.aging(asOf);
  }

  @GetMapping("/finance/reports/outstanding")
  @PreAuthorize("hasAuthority('fee:read')")
  @Operation(summary = "Outstanding fees by student account (RP-FI-03)")
  public List<OutstandingRow> outstanding() {
    return financeReportService.outstandingByStudent();
  }

  @GetMapping("/finance/reports/daily-collection")
  @PreAuthorize("hasAuthority('fee:read')")
  @Operation(summary = "Daily collection (RP-FI-01)")
  public List<DailyCollectionRow> dailyCollection(
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    return financeReportService.dailyCollection(from, to);
  }

  @GetMapping("/finance/reports/collection-by-method")
  @PreAuthorize("hasAuthority('fee:read')")
  @Operation(summary = "Collection by payment method (RP-FI-02)")
  public List<NamedAmount> byMethod(
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    return financeReportService.collectionByMethod(from, to);
  }

  @GetMapping("/finance/reports/collection-by-category")
  @PreAuthorize("hasAuthority('fee:read')")
  @Operation(summary = "Collection by fee category (RP-FI-08)")
  public List<NamedAmount> byCategory(
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    return financeReportService.collectionByCategory(from, to);
  }
}
