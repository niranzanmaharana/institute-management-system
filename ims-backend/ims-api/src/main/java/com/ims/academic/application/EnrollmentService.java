package com.ims.academic.application;

import com.ims.academic.api.EnrollmentResponse;
import com.ims.academic.domain.Enrollment;
import com.ims.academic.infrastructure.EnrollmentJpaRepository;
import com.ims.admissions.domain.AdmissionApplication;
import com.ims.admissions.infrastructure.AdmissionApplicationJpaRepository;
import com.ims.common.tenancy.TenantContext;
import com.ims.finance.api.FinanceDtos.ActivationResultResponse;
import com.ims.finance.api.FinanceDtos.FeeAccountResponse;
import com.ims.finance.api.FinanceDtos.InvoiceResponse;
import com.ims.finance.application.FinanceService;
import com.ims.people.student.application.StudentService;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnrollmentService {

  private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

  private final EnrollmentJpaRepository enrollmentJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final StudentService studentService;
  private final FinanceService financeService;
  private final AdmissionApplicationJpaRepository admissionApplicationJpaRepository;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;

  public EnrollmentService(
      EnrollmentJpaRepository enrollmentJpaRepository,
      AcademicCatalogService academicCatalogService,
      StudentService studentService,
      FinanceService financeService,
      AdmissionApplicationJpaRepository admissionApplicationJpaRepository,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService) {
    this.enrollmentJpaRepository = enrollmentJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.studentService = studentService;
    this.financeService = financeService;
    this.admissionApplicationJpaRepository = admissionApplicationJpaRepository;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
  }

  public record CreateEnrollmentCommand(
      @NotNull Long studentId, @NotNull Long batchId, @NotNull Long feePlanId) {}

  public record ActivateEnrollmentCommand(
      boolean capacityOverride,
      @Size(max = 512) String overrideReason,
      boolean admissionWaiver,
      @Size(max = 512) String waiverReason) {}

  public record ReasonCommand(@NotBlank @Size(max = 512) String reason) {}

  public record TransferEnrollmentCommand(
      @NotNull Long targetBatchId,
      Long newFeePlanId,
      boolean capacityOverride,
      @Size(max = 512) String overrideReason) {}

  @Transactional
  public EnrollmentResponse create(CreateEnrollmentCommand command) {
    long instituteId = requireTenant();
    studentService.get(command.studentId());

    AcademicCatalogService.BatchView batch = academicCatalogService.getBatch(command.batchId());
    AcademicCatalogService.FeePlanView feePlan =
        academicCatalogService.getFeePlan(command.feePlanId());

    if (!feePlan.courseId().equals(batch.courseId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Fee plan course must match batch course");
    }

    assertNoOpenEnrollmentForCourse(instituteId, command.studentId(), batch.courseId());

    Enrollment saved =
        enrollmentJpaRepository.save(
            Enrollment.create(
                instituteId,
                command.studentId(),
                command.batchId(),
                batch.courseId(),
                command.feePlanId()));
    log.info(
        "Enrollment created id={} studentId={} batchId={} instituteId={}",
        saved.getId(),
        saved.getStudentId(),
        saved.getBatchId(),
        instituteId);
    auditService.record(
        instituteId,
        "academic",
        "Enrollment",
        saved.getId(),
        "ENROLLMENT_CREATED",
        null,
        Map.of(
            "studentId", saved.getStudentId(),
            "batchId", saved.getBatchId(),
            "courseId", saved.getCourseId(),
            "status", saved.getStatus()),
        null);
    return toResponse(saved);
  }

  @Transactional
  public ActivationResultResponse activate(Long id, String idempotencyKey, ActivateEnrollmentCommand command) {
    ActivateEnrollmentCommand cmd =
        command == null
            ? new ActivateEnrollmentCommand(false, null, false, null)
            : command;
    Enrollment enrollment = requireEnrollment(id);
    if (enrollment.isActive()) {
      FeeAccountResponse account = financeService.getFeeAccountForEnrollment(enrollment.getId());
      List<InvoiceResponse> invoices =
          financeService.listInvoicesForEnrollment(enrollment.getId());
      log.info(
          "Enrollment activate idempotent id={} key={} instituteId={}",
          enrollment.getId(),
          idempotencyKey,
          enrollment.getInstituteId());
      return new ActivationResultResponse(
          enrollment.getId(),
          enrollment.getStatus(),
          enrollment.getActivatedAt(),
          account,
          invoices);
    }

    assertApplied(enrollment);
    assertAdmissionOrWaiver(enrollment, cmd.admissionWaiver(), cmd.waiverReason());
    assertBatchOpenWithCapacity(
        enrollment.getBatchId(),
        enrollment.getInstituteId(),
        cmd.capacityOverride(),
        cmd.overrideReason(),
        enrollment.getId());

    enrollment.activate();
    enrollmentJpaRepository.save(enrollment);
    FeeAccountResponse account = financeService.ensureFeeAccountWithInvoices(enrollment);
    List<InvoiceResponse> invoices =
        financeService.listInvoicesForEnrollment(enrollment.getId());
    log.info(
        "Enrollment activated id={} feeAccountId={} instituteId={}",
        enrollment.getId(),
        account.id(),
        enrollment.getInstituteId());
    Map<String, Object> after = new HashMap<>();
    after.put("feeAccountId", account.id());
    after.put("status", enrollment.getStatus());
    if (cmd.capacityOverride()) {
      after.put("capacityOverride", true);
    }
    if (cmd.admissionWaiver()) {
      after.put("admissionWaiver", true);
    }
    auditService.record(
        enrollment.getInstituteId(),
        "academic",
        "Enrollment",
        enrollment.getId(),
        "ENROLLMENT_ACTIVATED",
        null,
        after,
        firstNonBlank(cmd.overrideReason(), cmd.waiverReason()));
    return new ActivationResultResponse(
        enrollment.getId(),
        enrollment.getStatus(),
        enrollment.getActivatedAt(),
        account,
        invoices);
  }

  @Transactional
  public EnrollmentResponse cancel(Long id, ReasonCommand command) {
    Enrollment enrollment = requireEnrollment(id);
    Map<String, Object> before = statusSnapshot(enrollment);
    enrollment.cancel();
    enrollmentJpaRepository.save(enrollment);
    auditLifecycle(enrollment, "ENROLLMENT_CANCELLED", before, command.reason());
    return toResponse(enrollment);
  }

  @Transactional
  public EnrollmentResponse suspend(Long id, ReasonCommand command) {
    Enrollment enrollment = requireEnrollment(id);
    Map<String, Object> before = statusSnapshot(enrollment);
    enrollment.suspend();
    enrollmentJpaRepository.save(enrollment);
    auditLifecycle(enrollment, "ENROLLMENT_SUSPENDED", before, command.reason());
    return toResponse(enrollment);
  }

  @Transactional
  public EnrollmentResponse resume(Long id, ActivateEnrollmentCommand command) {
    ActivateEnrollmentCommand cmd =
        command == null
            ? new ActivateEnrollmentCommand(false, null, false, null)
            : command;
    Enrollment enrollment = requireEnrollment(id);
    Map<String, Object> before = statusSnapshot(enrollment);
    assertBatchOpenWithCapacity(
        enrollment.getBatchId(),
        enrollment.getInstituteId(),
        cmd.capacityOverride(),
        cmd.overrideReason(),
        enrollment.getId());
    enrollment.resume();
    enrollmentJpaRepository.save(enrollment);
    auditLifecycle(
        enrollment,
        "ENROLLMENT_RESUMED",
        before,
        cmd.capacityOverride() ? cmd.overrideReason() : null);
    return toResponse(enrollment);
  }

  @Transactional
  public EnrollmentResponse withdraw(Long id, ReasonCommand command) {
    Enrollment enrollment = requireEnrollment(id);
    Map<String, Object> before = statusSnapshot(enrollment);
    enrollment.withdraw();
    enrollmentJpaRepository.save(enrollment);
    auditLifecycle(enrollment, "ENROLLMENT_WITHDRAWN", before, command.reason());
    return toResponse(enrollment);
  }

  @Transactional
  public EnrollmentResponse complete(Long id, ReasonCommand command) {
    Enrollment enrollment = requireEnrollment(id);
    Map<String, Object> before = statusSnapshot(enrollment);
    enrollment.complete();
    enrollmentJpaRepository.save(enrollment);
    auditLifecycle(enrollment, "ENROLLMENT_COMPLETED", before, command.reason());
    return toResponse(enrollment);
  }

  @Transactional
  public EnrollmentResponse transfer(Long id, TransferEnrollmentCommand command) {
    Enrollment enrollment = requireEnrollment(id);
    if (!enrollment.isActive()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only ACTIVE enrollments can be transferred");
    }
    AcademicCatalogService.BatchView target =
        academicCatalogService.getBatch(command.targetBatchId());
    if (!target.courseId().equals(enrollment.getCourseId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Transfer target batch must be the same course (BR-TR-01)");
    }
    if (target.id().equals(enrollment.getBatchId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target batch is already current");
    }
    if (!"OPEN".equalsIgnoreCase(target.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Target batch is not OPEN");
    }

    Long newFeePlanId = command.newFeePlanId();
    boolean feePlanChanged = false;
    if (newFeePlanId != null && !newFeePlanId.equals(enrollment.getFeePlanId())) {
      requireAdminOrAccountant("Fee plan change on transfer requires ADMIN or ACCOUNTANT (BR-TR-03)");
      AcademicCatalogService.FeePlanView feePlan = academicCatalogService.getFeePlan(newFeePlanId);
      if (!feePlan.courseId().equals(enrollment.getCourseId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "New fee plan must match enrollment course");
      }
      feePlanChanged = true;
    } else {
      newFeePlanId = null;
    }

    assertBatchOpenWithCapacity(
        target.id(),
        enrollment.getInstituteId(),
        command.capacityOverride(),
        command.overrideReason(),
        enrollment.getId());

    Map<String, Object> before =
        Map.of(
            "batchId", enrollment.getBatchId(),
            "feePlanId", enrollment.getFeePlanId(),
            "status", enrollment.getStatus());
    Long fromBatchId = enrollment.getBatchId();
    enrollment.transferTo(target.id(), newFeePlanId);
    enrollmentJpaRepository.save(enrollment);

    Map<String, Object> after = new HashMap<>();
    after.put("fromBatchId", fromBatchId);
    after.put("toBatchId", enrollment.getBatchId());
    after.put("feePlanId", enrollment.getFeePlanId());
    after.put("feePlanChanged", feePlanChanged);
    if (command.capacityOverride()) {
      after.put("capacityOverride", true);
    }
    auditService.record(
        enrollment.getInstituteId(),
        "academic",
        "Enrollment",
        enrollment.getId(),
        "ENROLLMENT_TRANSFERRED",
        before,
        after,
        command.capacityOverride() ? command.overrideReason() : null);
    log.info(
        "Enrollment transferred id={} fromBatch={} toBatch={} instituteId={}",
        enrollment.getId(),
        fromBatchId,
        enrollment.getBatchId(),
        enrollment.getInstituteId());
    return toResponse(enrollment);
  }

  @Transactional(readOnly = true)
  public EnrollmentResponse get(Long id) {
    return toResponse(requireEnrollment(id));
  }

  @Transactional(readOnly = true)
  public List<EnrollmentResponse> list() {
    long instituteId = requireTenant();
    return enrollmentJpaRepository.findByInstituteIdOrderByCreatedAtDesc(instituteId).stream()
        .map(this::toResponse)
        .toList();
  }

  private void assertNoOpenEnrollmentForCourse(long instituteId, Long studentId, Long courseId) {
    if (enrollmentJpaRepository.existsByInstituteIdAndStudentIdAndCourseIdAndStatusIn(
        instituteId,
        studentId,
        courseId,
        List.of(
            Enrollment.STATUS_APPLIED,
            Enrollment.STATUS_ACTIVE,
            Enrollment.STATUS_SUSPENDED))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Student already has APPLIED/ACTIVE/SUSPENDED enrollment for this course (BR-EN-02)");
    }
  }

  private void assertApplied(Enrollment enrollment) {
    if (!Enrollment.STATUS_APPLIED.equals(enrollment.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only APPLIED enrollments can be activated");
    }
  }

  private void assertAdmissionOrWaiver(
      Enrollment enrollment, boolean admissionWaiver, String waiverReason) {
    boolean approved =
        admissionApplicationJpaRepository.existsByInstituteIdAndStudentIdAndCourseIdAndStatus(
            enrollment.getInstituteId(),
            enrollment.getStudentId(),
            enrollment.getCourseId(),
            AdmissionApplication.STATUS_APPROVED);
    if (approved) {
      return;
    }
    if (!admissionWaiver) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Approved admission required for this course, or ADMIN waiver (BR-EN-04)");
    }
    requireAdmin("Admission waiver requires ADMIN (BR-EN-04)");
    requireReason(waiverReason, "Waiver reason is required");
    auditService.record(
        enrollment.getInstituteId(),
        "academic",
        "Enrollment",
        enrollment.getId(),
        "ADMISSION_WAIVER",
        null,
        Map.of("courseId", enrollment.getCourseId(), "studentId", enrollment.getStudentId()),
        waiverReason.trim());
  }

  private void assertBatchOpenWithCapacity(
      Long batchId,
      Long instituteId,
      boolean capacityOverride,
      String overrideReason,
      Long enrollmentId) {
    AcademicCatalogService.BatchView batch = academicCatalogService.getBatch(batchId);
    if (!"OPEN".equalsIgnoreCase(batch.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch is not OPEN");
    }
    long activeCount =
        enrollmentJpaRepository.countByInstituteIdAndBatchIdAndStatus(
            instituteId, batchId, Enrollment.STATUS_ACTIVE);
    if (activeCount < batch.capacity()) {
      return;
    }
    if (!capacityOverride) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch capacity reached (BR-EN-03)");
    }
    requireAdmin("Capacity override requires ADMIN (BR-EN-03 / FR-AC-08)");
    requireReason(overrideReason, "Capacity override reason is required");
    auditService.record(
        instituteId,
        "academic",
        "Enrollment",
        enrollmentId,
        "CAPACITY_OVERRIDE",
        null,
        Map.of("batchId", batchId, "capacity", batch.capacity(), "activeCount", activeCount),
        overrideReason.trim());
  }

  private void requireAdmin(String message) {
    if (!hasRole("ADMIN")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
  }

  private void requireAdminOrAccountant(String message) {
    if (!hasRole("ADMIN") && !hasRole("ACCOUNTANT")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
  }

  private static boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    String want = "ROLE_" + role;
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (want.equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  private static void requireReason(String reason, String message) {
    if (reason == null || reason.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a.trim();
    }
    if (b != null && !b.isBlank()) {
      return b.trim();
    }
    return null;
  }

  private void auditLifecycle(
      Enrollment enrollment, String action, Map<String, Object> before, String reason) {
    auditService.record(
        enrollment.getInstituteId(),
        "academic",
        "Enrollment",
        enrollment.getId(),
        action,
        before,
        statusSnapshot(enrollment),
        reason);
    log.info(
        "{} id={} status={} instituteId={}",
        action,
        enrollment.getId(),
        enrollment.getStatus(),
        enrollment.getInstituteId());
  }

  private static Map<String, Object> statusSnapshot(Enrollment enrollment) {
    return Map.of(
        "status", enrollment.getStatus(),
        "batchId", enrollment.getBatchId(),
        "feePlanId", enrollment.getFeePlanId());
  }

  private Enrollment requireEnrollment(Long id) {
    long instituteId = requireTenant();
    return enrollmentJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private EnrollmentResponse toResponse(Enrollment enrollment) {
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getStudentId(),
        enrollment.getBatchId(),
        enrollment.getCourseId(),
        enrollment.getFeePlanId(),
        enrollment.getStatus(),
        enrollment.getActivatedAt(),
        enrollment.getSuspendedAt(),
        enrollment.getWithdrawnAt(),
        enrollment.getCompletedAt(),
        enrollment.getCancelledAt(),
        enrollment.getCreatedAt());
  }
}
