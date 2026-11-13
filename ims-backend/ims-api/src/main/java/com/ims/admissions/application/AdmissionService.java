package com.ims.admissions.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.admissions.api.AdmissionResponse;
import com.ims.admissions.domain.AdmissionApplication;
import com.ims.admissions.domain.Enquiry;
import com.ims.admissions.infrastructure.AdmissionApplicationJpaRepository;
import com.ims.admissions.infrastructure.EnquiryJpaRepository;
import com.ims.common.paging.PageResponse;
import com.ims.common.tenancy.TenantContext;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.people.student.application.StudentService.CreateStudentCommand;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdmissionService {

  private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

  private final AdmissionApplicationJpaRepository admissionApplicationJpaRepository;
  private final EnquiryJpaRepository enquiryJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final StudentService studentService;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;
  private final CodeGeneratorService codeGeneratorService;

  public AdmissionService(
      AdmissionApplicationJpaRepository admissionApplicationJpaRepository,
      EnquiryJpaRepository enquiryJpaRepository,
      AcademicCatalogService academicCatalogService,
      StudentService studentService,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService,
      CodeGeneratorService codeGeneratorService) {
    this.admissionApplicationJpaRepository = admissionApplicationJpaRepository;
    this.enquiryJpaRepository = enquiryJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.studentService = studentService;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
    this.codeGeneratorService = codeGeneratorService;
  }

  public record CreateAdmissionCommand(
      @Size(max = 32) String applicationNo,
      @NotNull Long courseId,
      @NotBlank @Size(max = 128) String applicantName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      Long enquiryId) {}

  public record CreateStudentOnApprove(
      @Size(max = 32) String studentCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email) {}

  public record ApproveAdmissionCommand(
      Long studentId, CreateStudentOnApprove createStudent, @Size(max = 512) String reason) {}

  public record ReasonCommand(@NotBlank @Size(min = 5, max = 512) String reason) {}

  @Transactional
  public AdmissionResponse create(CreateAdmissionCommand command) {
    long instituteId = requireTenant();
    academicCatalogService.getCourse(command.courseId());
    Long enquiryId = command.enquiryId();
    if (enquiryId != null) {
      Enquiry enquiry = requireEnquiry(enquiryId, instituteId);
      if (!Enquiry.STATUS_OPEN.equals(enquiry.getStatus())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Enquiry is not OPEN");
      }
    }

    String phone = blankToNull(command.phone());
    String email = blankToNull(command.email());
    assertNoActiveDuplicate(instituteId, command.courseId(), phone, email);

    String applicationNo =
        codeGeneratorService.resolve(CodeEntityType.ADMISSION, command.applicationNo());
    if (admissionApplicationJpaRepository.existsByInstituteIdAndApplicationNoIgnoreCase(
        instituteId, applicationNo)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Application number already exists");
    }

    AdmissionApplication saved =
        admissionApplicationJpaRepository.save(
            AdmissionApplication.create(
                instituteId,
                applicationNo,
                command.courseId(),
                command.applicantName(),
                command.phone(),
                command.email(),
                enquiryId));
    if (enquiryId != null) {
      Enquiry enquiry = requireEnquiry(enquiryId, instituteId);
      enquiry.convert(saved.getId());
      enquiryJpaRepository.save(enquiry);
    }
    log.info(
        "Admission created id={} no={} courseId={} instituteId={}",
        saved.getId(),
        saved.getApplicationNo(),
        saved.getCourseId(),
        instituteId);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<AdmissionResponse> list(String q, String status, int page, int size) {
    long instituteId = requireTenant();
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    String statusFilter =
        status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())
            ? null
            : status.trim().toUpperCase();
    Page<AdmissionApplication> result =
        admissionApplicationJpaRepository.search(
            instituteId,
            q == null ? "" : q.trim(),
            statusFilter,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    List<AdmissionResponse> content = result.getContent().stream().map(this::toResponse).toList();
    return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public AdmissionResponse get(Long id) {
    return toResponse(requireApplication(id));
  }

  @Transactional
  public AdmissionResponse approve(Long id, ApproveAdmissionCommand command, Long decidedByUserId) {
    AdmissionApplication app = requireApplication(id);
    if (app.isDecided()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission already decided");
    }

    Long studentId;
    if (command.studentId() != null) {
      studentService.get(command.studentId());
      studentId = command.studentId();
    } else if (command.createStudent() != null) {
      CreateStudentOnApprove cs = command.createStudent();
      StudentResponse created =
          studentService.create(
              new CreateStudentCommand(
                  cs.studentCode(),
                  cs.firstName(),
                  cs.lastName(),
                  cs.phone(),
                  cs.email(),
                  null,
                  null));
      studentId = created.id();
    } else {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Either studentId or createStudent is required");
    }

    app.approve(studentId, decidedByUserId, command.reason());
    admissionApplicationJpaRepository.save(app);
    log.info(
        "Admission approved id={} studentId={} decidedBy={} instituteId={}",
        app.getId(),
        studentId,
        decidedByUserId,
        app.getInstituteId());
    auditService.record(
        app.getInstituteId(),
        "admissions",
        "AdmissionApplication",
        app.getId(),
        "ADMISSION_APPROVED",
        null,
        Map.of("studentId", studentId, "status", app.getStatus()),
        command.reason());
    return toResponse(app);
  }

  @Transactional
  public AdmissionResponse reject(Long id, ReasonCommand command, Long decidedByUserId) {
    AdmissionApplication app = requireApplication(id);
    if (app.isDecided()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission already decided");
    }
    app.reject(decidedByUserId, command.reason());
    admissionApplicationJpaRepository.save(app);
    auditService.record(
        app.getInstituteId(),
        "admissions",
        "AdmissionApplication",
        app.getId(),
        "ADMISSION_REJECTED",
        null,
        Map.of("status", app.getStatus()),
        command.reason());
    log.info(
        "Admission rejected id={} decidedBy={} instituteId={}",
        app.getId(),
        decidedByUserId,
        app.getInstituteId());
    return toResponse(app);
  }

  @Transactional
  public AdmissionResponse cancel(Long id, ReasonCommand command, Long decidedByUserId) {
    AdmissionApplication app = requireApplication(id);
    if (app.isDecided()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission already decided");
    }
    app.cancel(decidedByUserId, command.reason());
    admissionApplicationJpaRepository.save(app);
    auditService.record(
        app.getInstituteId(),
        "admissions",
        "AdmissionApplication",
        app.getId(),
        "ADMISSION_CANCELLED",
        null,
        Map.of("status", app.getStatus()),
        command.reason());
    log.info(
        "Admission cancelled id={} decidedBy={} instituteId={}",
        app.getId(),
        decidedByUserId,
        app.getInstituteId());
    return toResponse(app);
  }

  private void assertNoActiveDuplicate(long instituteId, Long courseId, String phone, String email) {
    if (phone != null
        && admissionApplicationJpaRepository.existsByInstituteIdAndCourseIdAndPhoneAndStatus(
            instituteId, courseId, phone, AdmissionApplication.STATUS_SUBMITTED)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Active application already exists for this phone and course (BR-AD-04)");
    }
    if (email != null
        && admissionApplicationJpaRepository.existsByInstituteIdAndCourseIdAndEmailIgnoreCaseAndStatus(
            instituteId, courseId, email, AdmissionApplication.STATUS_SUBMITTED)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Active application already exists for this email and course (BR-AD-04)");
    }
  }

  private Enquiry requireEnquiry(Long enquiryId, long instituteId) {
    return enquiryJpaRepository
        .findByIdAndInstituteId(enquiryId, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found"));
  }

  private AdmissionApplication requireApplication(Long id) {
    long instituteId = requireTenant();
    return admissionApplicationJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private AdmissionResponse toResponse(AdmissionApplication app) {
    return new AdmissionResponse(
        app.getId(),
        app.getApplicationNo(),
        app.getEnquiryId(),
        app.getCourseId(),
        app.getApplicantName(),
        app.getPhone(),
        app.getEmail(),
        app.getStatus(),
        app.getStudentId(),
        app.getDecidedBy(),
        app.getDecisionReason(),
        app.getDecidedAt(),
        app.getCreatedAt());
  }
}
