package com.ims.people.student.application;

import com.ims.common.paging.PageResponse;
import com.ims.common.tenancy.TenantContext;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.api.StudentResponse.AddressResponse;
import com.ims.people.student.api.StudentResponse.GuardianLinkResponse;
import com.ims.people.student.domain.Guardian;
import com.ims.people.student.domain.Student;
import com.ims.people.student.domain.StudentAddress;
import com.ims.people.student.domain.StudentGuardian;
import com.ims.people.student.infrastructure.GuardianJpaRepository;
import com.ims.people.student.infrastructure.StudentAddressJpaRepository;
import com.ims.people.student.infrastructure.StudentGuardianJpaRepository;
import com.ims.people.student.infrastructure.StudentJpaRepository;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {

  private static final Logger log = LoggerFactory.getLogger(StudentService.class);

  private final StudentJpaRepository studentJpaRepository;
  private final GuardianJpaRepository guardianJpaRepository;
  private final StudentGuardianJpaRepository studentGuardianJpaRepository;
  private final StudentAddressJpaRepository studentAddressJpaRepository;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;
  private final CodeGeneratorService codeGeneratorService;

  public StudentService(
      StudentJpaRepository studentJpaRepository,
      GuardianJpaRepository guardianJpaRepository,
      StudentGuardianJpaRepository studentGuardianJpaRepository,
      StudentAddressJpaRepository studentAddressJpaRepository,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService,
      CodeGeneratorService codeGeneratorService) {
    this.studentJpaRepository = studentJpaRepository;
    this.guardianJpaRepository = guardianJpaRepository;
    this.studentGuardianJpaRepository = studentGuardianJpaRepository;
    this.studentAddressJpaRepository = studentAddressJpaRepository;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
    this.codeGeneratorService = codeGeneratorService;
  }

  public record GuardianInput(
      @NotBlank @Size(max = 128) String name,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @NotBlank @Size(max = 64) String relation,
      boolean primaryGuardian) {}

  public record AddressInput(
      @NotBlank @Size(max = 255) String line1,
      @Size(max = 255) String line2,
      @NotBlank @Size(max = 64) String city,
      @Size(max = 64) String state,
      @Size(max = 16) String postalCode,
      @Size(max = 64) String country,
      boolean primaryAddress) {}

  public record CreateStudentCommand(
      @Size(max = 32) String studentCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      List<GuardianInput> guardians,
      List<AddressInput> addresses) {}

  public record UpdateStudentCommand(
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      List<GuardianInput> guardians,
      List<AddressInput> addresses) {}

  @Transactional(readOnly = true)
  public PageResponse<StudentResponse> list(String q, String status, int page, int size) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    Page<Student> result =
        studentJpaRepository.search(
            instituteId,
            q == null ? "" : q.trim(),
            normalizeStatusFilter(status),
            PageRequest.of(safePage, safeSize));
    List<StudentResponse> content =
        result.getContent().stream().map(this::toSummaryResponse).toList();
    return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public StudentResponse get(Long id) {
    return toDetailResponse(requireActiveStudent(id));
  }

  @Transactional
  public StudentResponse create(CreateStudentCommand command) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    String code = codeGeneratorService.resolve(CodeEntityType.STUDENT, command.studentCode());
    if (studentJpaRepository.existsByInstituteIdAndStudentCodeIgnoreCase(instituteId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Student code already exists");
    }
    Student student =
        studentJpaRepository.save(
            Student.create(
                instituteId,
                code,
                command.firstName(),
                command.lastName(),
                command.phone(),
                command.email()));
    replaceGuardians(instituteId, student.getId(), command.guardians());
    replaceAddresses(instituteId, student.getId(), command.addresses());
    log.info(
        "Student created id={} code={} instituteId={}",
        student.getId(),
        student.getStudentCode(),
        instituteId);
    auditService.record(
        instituteId,
        "people",
        "Student",
        student.getId(),
        "STUDENT_CREATED",
        null,
        Map.of("studentCode", student.getStudentCode(), "status", student.getStatus()),
        null);
    return toDetailResponse(student);
  }

  @Transactional
  public StudentResponse update(Long id, UpdateStudentCommand command) {
    Student student = requireActiveStudent(id);
    long instituteId = student.getInstituteId();
    student.update(command.firstName(), command.lastName(), command.phone(), command.email());
    studentJpaRepository.save(student);
    replaceGuardians(instituteId, student.getId(), command.guardians());
    replaceAddresses(instituteId, student.getId(), command.addresses());
    log.info("Student updated id={} code={} instituteId={}", student.getId(), student.getStudentCode(), instituteId);
    return toDetailResponse(student);
  }

  @Transactional
  public StudentResponse deactivate(Long id) {
    Student student = requireActiveStudent(id);
    student.deactivate();
    studentJpaRepository.save(student);
    log.info(
        "Student deactivated id={} code={} instituteId={}",
        student.getId(),
        student.getStudentCode(),
        student.getInstituteId());
    return toDetailResponse(student);
  }

  private Student requireActiveStudent(Long id) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return studentJpaRepository
        .findByIdAndInstituteIdAndDeletedAtIsNull(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
  }

  private void replaceGuardians(long instituteId, Long studentId, List<GuardianInput> inputs) {
    if (inputs == null) {
      return;
    }
    studentGuardianJpaRepository.deleteByStudentIdAndInstituteId(studentId, instituteId);
    if (inputs.isEmpty()) {
      return;
    }
    for (GuardianInput input : inputs) {
      Guardian guardian = findOrCreateGuardian(instituteId, input);
      studentGuardianJpaRepository.save(
          new StudentGuardian(
              instituteId,
              studentId,
              guardian.getId(),
              input.relation(),
              input.primaryGuardian()));
    }
  }

  private Guardian findOrCreateGuardian(long instituteId, GuardianInput input) {
    Optional<Guardian> byPhone =
        input.phone() == null || input.phone().isBlank()
            ? Optional.empty()
            : guardianJpaRepository.findByInstituteIdAndPhoneAndDeletedAtIsNull(
                instituteId, input.phone().trim());
    if (byPhone.isPresent()) {
      return byPhone.get();
    }
    Optional<Guardian> byEmail =
        input.email() == null || input.email().isBlank()
            ? Optional.empty()
            : guardianJpaRepository.findByInstituteIdAndEmailIgnoreCaseAndDeletedAtIsNull(
                instituteId, input.email().trim());
    if (byEmail.isPresent()) {
      return byEmail.get();
    }
    return guardianJpaRepository.save(
        Guardian.create(instituteId, input.name(), input.phone(), input.email()));
  }

  private void replaceAddresses(long instituteId, Long studentId, List<AddressInput> inputs) {
    if (inputs == null) {
      return;
    }
    studentAddressJpaRepository.deleteByStudentIdAndInstituteId(studentId, instituteId);
    if (inputs.isEmpty()) {
      return;
    }
    for (AddressInput input : inputs) {
      studentAddressJpaRepository.save(
          StudentAddress.create(
              instituteId,
              studentId,
              input.line1(),
              input.line2(),
              input.city(),
              input.state(),
              input.postalCode(),
              input.country(),
              input.primaryAddress()));
    }
  }

  private StudentResponse toSummaryResponse(Student student) {
    return new StudentResponse(
        student.getId(),
        student.getStudentCode(),
        student.getFirstName(),
        student.getLastName(),
        student.getPhone(),
        student.getEmail(),
        student.getStatus(),
        List.of(),
        List.of());
  }

  private StudentResponse toDetailResponse(Student student) {
    long instituteId = student.getInstituteId();
    List<StudentGuardian> links =
        studentGuardianJpaRepository.findByStudentIdAndInstituteId(student.getId(), instituteId);
    List<GuardianLinkResponse> guardians = new ArrayList<>();
    for (StudentGuardian link : links) {
      Guardian guardian =
          guardianJpaRepository
              .findByIdAndInstituteIdAndDeletedAtIsNull(link.getGuardianId(), instituteId)
              .orElse(null);
      if (guardian == null) {
        continue;
      }
      guardians.add(
          new GuardianLinkResponse(
              guardian.getId(),
              guardian.getName(),
              guardian.getPhone(),
              guardian.getEmail(),
              link.getRelation(),
              link.isPrimaryGuardian()));
    }
    List<AddressResponse> addresses =
        studentAddressJpaRepository.findByStudentIdAndInstituteId(student.getId(), instituteId)
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
    return new StudentResponse(
        student.getId(),
        student.getStudentCode(),
        student.getFirstName(),
        student.getLastName(),
        student.getPhone(),
        student.getEmail(),
        student.getStatus(),
        guardians,
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
