package com.ims.finance.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.finance.domain.FinancialYear;
import com.ims.finance.infrastructure.FinancialYearJpaRepository;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FinancialYearService {

  private final FinancialYearJpaRepository financialYearJpaRepository;
  private final TenantFilterEnabler tenantFilterEnabler;

  public FinancialYearService(
      FinancialYearJpaRepository financialYearJpaRepository,
      TenantFilterEnabler tenantFilterEnabler) {
    this.financialYearJpaRepository = financialYearJpaRepository;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record FinancialYearView(
      Long id, String name, LocalDate startDate, LocalDate endDate, boolean active) {}

  public record CreateFinancialYearCommand(
      @NotBlank @Size(max = 64) String name,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      boolean activate) {}

  @Transactional(readOnly = true)
  public List<FinancialYearView> list() {
    long instituteId = requireTenant();
    return financialYearJpaRepository.findByInstituteIdOrderByStartDateDesc(instituteId).stream()
        .map(this::toView)
        .toList();
  }

  @Transactional
  public FinancialYearView create(CreateFinancialYearCommand command) {
    long instituteId = requireTenant();
    if (command.endDate().isBefore(command.startDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
    }
    String name = command.name().trim();
    if (financialYearJpaRepository.existsByInstituteIdAndNameIgnoreCase(instituteId, name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Financial year name already exists");
    }
    boolean activate = command.activate();
    if (activate) {
      financialYearJpaRepository.deactivateAll(instituteId);
    } else if (financialYearJpaRepository.findFirstByInstituteIdAndActiveTrue(instituteId).isEmpty()) {
      activate = true;
    }
    FinancialYear saved =
        financialYearJpaRepository.save(
            FinancialYear.create(instituteId, name, command.startDate(), command.endDate(), activate));
    return toView(saved);
  }

  @Transactional
  public FinancialYearView activate(Long id) {
    long instituteId = requireTenant();
    FinancialYear year =
        financialYearJpaRepository
            .findByIdAndInstituteId(id, instituteId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Financial year not found"));
    financialYearJpaRepository.deactivateAll(instituteId);
    year.markActive(true);
    return toView(financialYearJpaRepository.save(year));
  }

  private FinancialYearView toView(FinancialYear year) {
    return new FinancialYearView(
        year.getId(), year.getName(), year.getStartDate(), year.getEndDate(), year.isActive());
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }
}
