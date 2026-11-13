package com.ims.people.faculty.application;

import com.ims.common.paging.PageResponse;
import com.ims.common.tenancy.TenantContext;
import com.ims.people.faculty.api.FacultyResponse;
import com.ims.people.faculty.api.FacultyResponse.AddressResponse;
import com.ims.people.faculty.domain.Faculty;
import com.ims.people.faculty.domain.FacultyAddress;
import com.ims.people.faculty.infrastructure.FacultyAddressJpaRepository;
import com.ims.people.faculty.infrastructure.FacultyJpaRepository;
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
public class FacultyService {

  private static final Logger log = LoggerFactory.getLogger(FacultyService.class);

  private final FacultyJpaRepository facultyJpaRepository;
  private final FacultyAddressJpaRepository facultyAddressJpaRepository;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final CodeGeneratorService codeGeneratorService;

  public FacultyService(
      FacultyJpaRepository facultyJpaRepository,
      FacultyAddressJpaRepository facultyAddressJpaRepository,
      TenantFilterEnabler tenantFilterEnabler,
      CodeGeneratorService codeGeneratorService) {
    this.facultyJpaRepository = facultyJpaRepository;
    this.facultyAddressJpaRepository = facultyAddressJpaRepository;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.codeGeneratorService = codeGeneratorService;
  }

  public record AddressInput(
      @NotBlank @Size(max = 255) String line1,
      @Size(max = 255) String line2,
      @NotBlank @Size(max = 64) String city,
      @Size(max = 64) String state,
      @Size(max = 16) String postalCode,
      @Size(max = 64) String country,
      boolean primaryAddress) {}

  public record CreateFacultyCommand(
      @Size(max = 32) String facultyCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String department,
      List<AddressInput> addresses) {}

  public record FacultyRef(
      Long id, String facultyCode, String firstName, String lastName, String status) {}

  public record UpdateFacultyCommand(
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String department,
      List<AddressInput> addresses) {}

  @Transactional(readOnly = true)
  public PageResponse<FacultyResponse> list(String q, String status, int page, int size) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    Page<Faculty> result =
        facultyJpaRepository.search(
            instituteId,
            q == null ? "" : q.trim(),
            normalizeStatusFilter(status),
            PageRequest.of(safePage, safeSize));
    List<FacultyResponse> content =
        result.getContent().stream().map(this::toSummaryResponse).toList();
    return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public FacultyResponse get(Long id) {
    return toDetailResponse(requireActiveFaculty(id));
  }

  @Transactional(readOnly = true)
  public FacultyRef requireActiveNamed(Long id) {
    Faculty faculty = requireActiveFaculty(id);
    if (!"ACTIVE".equalsIgnoreCase(faculty.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty is not active");
    }
    return toRef(faculty);
  }

  @Transactional(readOnly = true)
  public java.util.Optional<FacultyRef> findNamed(Long id) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return facultyJpaRepository
        .findByIdAndInstituteIdAndDeletedAtIsNull(id, instituteId)
        .map(this::toRef);
  }

  @Transactional
  public FacultyResponse create(CreateFacultyCommand command) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    String code = codeGeneratorService.resolve(CodeEntityType.FACULTY, command.facultyCode());
    if (facultyJpaRepository.existsByInstituteIdAndFacultyCodeIgnoreCase(instituteId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Faculty code already exists");
    }
    Faculty faculty =
        facultyJpaRepository.save(
            Faculty.create(
                instituteId,
                code,
                command.firstName(),
                command.lastName(),
                command.phone(),
                command.email(),
                command.department()));
    replaceAddresses(instituteId, faculty.getId(), command.addresses());
    log.info(
        "Faculty created id={} code={} instituteId={}",
        faculty.getId(),
        faculty.getFacultyCode(),
        instituteId);
    return toDetailResponse(faculty);
  }

  @Transactional
  public FacultyResponse update(Long id, UpdateFacultyCommand command) {
    Faculty faculty = requireActiveFaculty(id);
    long instituteId = faculty.getInstituteId();
    faculty.update(
        command.firstName(),
        command.lastName(),
        command.phone(),
        command.email(),
        command.department());
    facultyJpaRepository.save(faculty);
    replaceAddresses(instituteId, faculty.getId(), command.addresses());
    log.info(
        "Faculty updated id={} code={} instituteId={}",
        faculty.getId(),
        faculty.getFacultyCode(),
        instituteId);
    return toDetailResponse(faculty);
  }

  @Transactional
  public FacultyResponse deactivate(Long id) {
    Faculty faculty = requireActiveFaculty(id);
    faculty.deactivate();
    facultyJpaRepository.save(faculty);
    log.info(
        "Faculty deactivated id={} code={} instituteId={}",
        faculty.getId(),
        faculty.getFacultyCode(),
        faculty.getInstituteId());
    return toDetailResponse(faculty);
  }

  private Faculty requireActiveFaculty(Long id) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return facultyJpaRepository
        .findByIdAndInstituteIdAndDeletedAtIsNull(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found"));
  }

  private void replaceAddresses(long instituteId, Long facultyId, List<AddressInput> inputs) {
    if (inputs == null) {
      return;
    }
    facultyAddressJpaRepository.deleteByFacultyIdAndInstituteId(facultyId, instituteId);
    if (inputs.isEmpty()) {
      return;
    }
    for (AddressInput input : inputs) {
      facultyAddressJpaRepository.save(
          FacultyAddress.create(
              instituteId,
              facultyId,
              input.line1(),
              input.line2(),
              input.city(),
              input.state(),
              input.postalCode(),
              input.country(),
              input.primaryAddress()));
    }
  }

  private FacultyRef toRef(Faculty faculty) {
    return new FacultyRef(
        faculty.getId(),
        faculty.getFacultyCode(),
        faculty.getFirstName(),
        faculty.getLastName(),
        faculty.getStatus());
  }

  private FacultyResponse toSummaryResponse(Faculty faculty) {
    return new FacultyResponse(
        faculty.getId(),
        faculty.getFacultyCode(),
        faculty.getFirstName(),
        faculty.getLastName(),
        faculty.getPhone(),
        faculty.getEmail(),
        faculty.getDepartment(),
        faculty.getStatus(),
        List.of());
  }

  private FacultyResponse toDetailResponse(Faculty faculty) {
    List<AddressResponse> addresses =
        facultyAddressJpaRepository
            .findByFacultyIdAndInstituteId(faculty.getId(), faculty.getInstituteId())
            .stream()
            .map(
                a ->
                    new AddressResponse(
                        a.getId(),
                        a.getLine1(),
                        a.getLine2(),
                        a.getCity(),
                        a.getState(),
                        a.getPostalCode(),
                        a.getCountry(),
                        a.isPrimaryAddress()))
            .toList();
    return new FacultyResponse(
        faculty.getId(),
        faculty.getFacultyCode(),
        faculty.getFirstName(),
        faculty.getLastName(),
        faculty.getPhone(),
        faculty.getEmail(),
        faculty.getDepartment(),
        faculty.getStatus(),
        addresses);
  }

  /** null = default list (non-deleted); ACTIVE / INACTIVE for explicit filters. */
  private static String normalizeStatusFilter(String status) {
    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
      return null;
    }
    return status.trim().toUpperCase();
  }
}
