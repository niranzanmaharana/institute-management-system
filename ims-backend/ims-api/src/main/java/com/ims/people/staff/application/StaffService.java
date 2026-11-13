package com.ims.people.staff.application;

import com.ims.common.paging.PageResponse;
import com.ims.common.tenancy.TenantContext;
import com.ims.people.staff.api.StaffResponse;
import com.ims.people.staff.domain.Staff;
import com.ims.people.staff.infrastructure.StaffJpaRepository;
import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffService {

  private static final Logger log = LoggerFactory.getLogger(StaffService.class);

  private final StaffJpaRepository staffJpaRepository;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final CodeGeneratorService codeGeneratorService;

  public StaffService(
      StaffJpaRepository staffJpaRepository,
      TenantFilterEnabler tenantFilterEnabler,
      CodeGeneratorService codeGeneratorService) {
    this.staffJpaRepository = staffJpaRepository;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.codeGeneratorService = codeGeneratorService;
  }

  public record CreateStaffCommand(
      @Size(max = 32) String staffCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String designation) {}

  public record UpdateStaffCommand(
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String designation) {}

  @Transactional(readOnly = true)
  public PageResponse<StaffResponse> list(String q, String status, int page, int size) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    Page<Staff> result =
        staffJpaRepository.search(
            instituteId,
            q == null ? "" : q.trim(),
            normalizeStatusFilter(status),
            PageRequest.of(safePage, safeSize));
    List<StaffResponse> content = result.getContent().stream().map(this::toResponse).toList();
    return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public StaffResponse get(Long id) {
    return toResponse(requireActiveStaff(id));
  }

  @Transactional
  public StaffResponse create(CreateStaffCommand command) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    String code = codeGeneratorService.resolve(CodeEntityType.STAFF, command.staffCode());
    if (staffJpaRepository.existsByInstituteIdAndStaffCodeIgnoreCase(instituteId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff code already exists");
    }
    Staff staff =
        staffJpaRepository.save(
            Staff.create(
                instituteId,
                code,
                command.firstName(),
                command.lastName(),
                command.phone(),
                command.email(),
                command.designation()));
    log.info(
        "Staff created id={} code={} instituteId={}",
        staff.getId(),
        staff.getStaffCode(),
        instituteId);
    return toResponse(staff);
  }

  @Transactional
  public StaffResponse update(Long id, UpdateStaffCommand command) {
    Staff staff = requireActiveStaff(id);
    staff.update(
        command.firstName(),
        command.lastName(),
        command.phone(),
        command.email(),
        command.designation());
    staffJpaRepository.save(staff);
    log.info(
        "Staff updated id={} code={} instituteId={}",
        staff.getId(),
        staff.getStaffCode(),
        staff.getInstituteId());
    return toResponse(staff);
  }

  @Transactional
  public StaffResponse deactivate(Long id) {
    Staff staff = requireActiveStaff(id);
    staff.deactivate();
    staffJpaRepository.save(staff);
    log.info(
        "Staff deactivated id={} code={} instituteId={}",
        staff.getId(),
        staff.getStaffCode(),
        staff.getInstituteId());
    return toResponse(staff);
  }

  private Staff requireActiveStaff(Long id) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return staffJpaRepository
        .findByIdAndInstituteIdAndDeletedAtIsNull(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
  }

  private StaffResponse toResponse(Staff staff) {
    return new StaffResponse(
        staff.getId(),
        staff.getStaffCode(),
        staff.getFirstName(),
        staff.getLastName(),
        staff.getPhone(),
        staff.getEmail(),
        staff.getDesignation(),
        staff.getStatus());
  }

  /** null = default list (non-deleted); ACTIVE / INACTIVE for explicit filters. */
  private static String normalizeStatusFilter(String status) {
    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
      return null;
    }
    return status.trim().toUpperCase();
  }
}
