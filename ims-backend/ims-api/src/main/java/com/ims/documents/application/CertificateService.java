package com.ims.documents.application;

import com.ims.academic.api.EnrollmentResponse;
import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.AcademicCatalogService.BatchView;
import com.ims.academic.application.AcademicCatalogService.CourseView;
import com.ims.academic.application.EnrollmentService;
import com.ims.academic.domain.Enrollment;
import com.ims.common.tenancy.TenantContext;
import com.ims.documents.api.CertificateDtos.CertificateDownloadResponse;
import com.ims.documents.api.CertificateDtos.CertificateTemplateResponse;
import com.ims.documents.api.CertificateDtos.GeneratedDocumentResponse;
import com.ims.documents.application.CertificatePdfRenderer.CourseLine;
import com.ims.documents.application.CertificatePdfRenderer.Letterhead;
import com.ims.documents.application.CertificatePdfRenderer.StudentLine;
import com.ims.documents.domain.DocumentTemplate;
import com.ims.documents.domain.GeneratedDocument;
import com.ims.documents.infrastructure.DocumentTemplateJpaRepository;
import com.ims.documents.infrastructure.GeneratedDocumentJpaRepository;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.institute.api.InstituteResponse;
import com.ims.platform.institute.application.InstituteService;
import com.ims.platform.storage.ObjectStorage;
import com.ims.platform.storage.StorageProperties;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CertificateService {

  public static final String CONTENT_TYPE_PDF = "application/pdf";

  private final DocumentTemplateJpaRepository templateJpaRepository;
  private final GeneratedDocumentJpaRepository generatedDocumentJpaRepository;
  private final StudentService studentService;
  private final EnrollmentService enrollmentService;
  private final AcademicCatalogService academicCatalogService;
  private final InstituteService instituteService;
  private final CertificatePdfRenderer pdfRenderer;
  private final ObjectStorage objectStorage;
  private final StorageProperties storageProperties;
  private final UserAccountRepository userAccountRepository;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public CertificateService(
      DocumentTemplateJpaRepository templateJpaRepository,
      GeneratedDocumentJpaRepository generatedDocumentJpaRepository,
      StudentService studentService,
      EnrollmentService enrollmentService,
      AcademicCatalogService academicCatalogService,
      InstituteService instituteService,
      CertificatePdfRenderer pdfRenderer,
      ObjectStorage objectStorage,
      StorageProperties storageProperties,
      UserAccountRepository userAccountRepository,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.templateJpaRepository = templateJpaRepository;
    this.generatedDocumentJpaRepository = generatedDocumentJpaRepository;
    this.studentService = studentService;
    this.enrollmentService = enrollmentService;
    this.academicCatalogService = academicCatalogService;
    this.instituteService = instituteService;
    this.pdfRenderer = pdfRenderer;
    this.objectStorage = objectStorage;
    this.storageProperties = storageProperties;
    this.userAccountRepository = userAccountRepository;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record GenerateCommand(Long studentId, String templateCode, Long enrollmentId) {}

  @Transactional(readOnly = true)
  public List<CertificateTemplateResponse> listTemplates() {
    requireTenant();
    return templateJpaRepository.findByActiveTrueOrderByCodeAsc().stream()
        .map(t -> new CertificateTemplateResponse(t.getCode(), t.getName(), t.isActive()))
        .toList();
  }

  @Transactional
  public GeneratedDocumentResponse generate(GenerateCommand command) {
    long instituteId = requireTenant();
    assertCanGenerate();
    if (command.studentId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
    }
    DocumentTemplate template = requireActiveTemplate(command.templateCode());
    StudentResponse student = studentService.get(command.studentId());
    if (!"ACTIVE".equalsIgnoreCase(student.status())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Certificates can only be issued for ACTIVE students");
    }

    EnrollmentResponse enrollment = resolveEnrollment(command, template.getCode(), student.id());
    BatchView batch = academicCatalogService.getBatch(enrollment.batchId());
    CourseView course = academicCatalogService.getCourse(batch.courseId());
    InstituteResponse institute = currentInstitute();

    Letterhead letterhead = toLetterhead(institute);
    StudentLine studentLine =
        new StudentLine(
            (student.firstName() + " " + student.lastName()).trim(), student.studentCode());
    CourseLine courseLine =
        new CourseLine(course.name(), batch.name(), batch.startDate(), batch.endDate());

    byte[] pdf;
    if (DocumentTemplate.CODE_BONAFIDE.equals(template.getCode())) {
      pdf = pdfRenderer.renderBonafide(letterhead, studentLine, courseLine);
    } else if (DocumentTemplate.CODE_COMPLETION.equals(template.getCode())) {
      pdf = pdfRenderer.renderCompletion(letterhead, studentLine, courseLine);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported template");
    }

    String fileName =
        template.getCode().toLowerCase(Locale.ROOT)
            + "-"
            + student.studentCode().replaceAll("[^A-Za-z0-9_-]", "_")
            + ".pdf";
    String storageKey =
        "institutes/"
            + instituteId
            + "/certificates/"
            + student.id()
            + "/"
            + UUID.randomUUID()
            + "/"
            + fileName;

    objectStorage.put(storageKey, CONTENT_TYPE_PDF, pdf);

    GeneratedDocument saved =
        generatedDocumentJpaRepository.save(
            GeneratedDocument.create(
                instituteId,
                template.getCode(),
                student.id(),
                enrollment.id(),
                storageKey,
                fileName,
                CONTENT_TYPE_PDF,
                pdf.length,
                currentUserIdOrNull()));

    auditService.record(
        instituteId,
        "documents",
        "GeneratedDocument",
        saved.getId(),
        "CERTIFICATE_GENERATED",
        null,
        Map.of(
            "templateCode", saved.getTemplateCode(),
            "studentId", saved.getStudentId(),
            "enrollmentId", saved.getEnrollmentId() == null ? "" : saved.getEnrollmentId()),
        null);

    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<GeneratedDocumentResponse> listForStudent(Long studentId) {
    long instituteId = requireTenant();
    assertCanReadStudent(studentId);
    studentService.get(studentId);
    return generatedDocumentJpaRepository
        .findByInstituteIdAndStudentIdOrderByIssuedAtDescIdDesc(instituteId, studentId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public CertificateDownloadResponse download(Long id) {
    long instituteId = requireTenant();
    GeneratedDocument document =
        generatedDocumentJpaRepository
            .findByIdAndInstituteId(id, instituteId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found"));
    assertCanReadStudent(document.getStudentId());
    String expectedPrefix = "institutes/" + instituteId + "/";
    if (!document.getStorageKey().startsWith(expectedPrefix)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate not found");
    }
    Duration expiry = Duration.ofMinutes(Math.max(1, storageProperties.presignExpiryMinutes()));
    String url =
        objectStorage.presignGet(
            document.getStorageKey(),
            document.getFileName(),
            document.getContentType(),
            expiry,
            true);
    return new CertificateDownloadResponse(url, expiry.toSeconds());
  }

  private EnrollmentResponse resolveEnrollment(
      GenerateCommand command, String templateCode, Long studentId) {
    if (DocumentTemplate.CODE_COMPLETION.equals(templateCode)) {
      if (command.enrollmentId() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "enrollmentId is required for COMPLETION certificates");
      }
      EnrollmentResponse enrollment = enrollmentService.get(command.enrollmentId());
      if (!studentId.equals(enrollment.studentId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Enrollment does not belong to this student");
      }
      if (!Enrollment.STATUS_COMPLETED.equals(enrollment.status())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Completion certificate requires a COMPLETED enrollment");
      }
      return enrollment;
    }

    // BONAFIDE — prefer explicit enrollment, else any non-cancelled enrollment for student
    if (command.enrollmentId() != null) {
      EnrollmentResponse enrollment = enrollmentService.get(command.enrollmentId());
      if (!studentId.equals(enrollment.studentId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Enrollment does not belong to this student");
      }
      return enrollment;
    }
    return enrollmentService.list().stream()
        .filter(e -> studentId.equals(e.studentId()))
        .filter(e -> !Enrollment.STATUS_CANCELLED.equals(e.status()))
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student has no enrollment to issue a bonafide certificate"));
  }

  private DocumentTemplate requireActiveTemplate(String templateCode) {
    if (templateCode == null || templateCode.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateCode is required");
    }
    DocumentTemplate template =
        templateJpaRepository
            .findByCodeIgnoreCase(templateCode.trim())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown template"));
    if (!template.isActive()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Template is inactive");
    }
    return template;
  }

  private Letterhead toLetterhead(InstituteResponse institute) {
    String address =
        joinNonBlank(institute.addressLine1(), institute.addressLine2());
    String cityState =
        joinNonBlank(
            institute.city(),
            institute.state(),
            institute.postalCode(),
            institute.country());
    String contact = joinNonBlank(institute.mobile(), institute.adminEmail(), institute.website());
    return new Letterhead(institute.name(), address, cityState, contact);
  }

  private InstituteResponse currentInstitute() {
    long instituteId = TenantContext.requireInstituteId();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof ImsUserPrincipal principal) {
      return instituteService.getVisible(instituteId, principal);
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
  }

  private static String joinNonBlank(String... parts) {
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append(part.trim());
    }
    return sb.isEmpty() ? null : sb.toString();
  }

  private GeneratedDocumentResponse toResponse(GeneratedDocument row) {
    return new GeneratedDocumentResponse(
        row.getId(),
        row.getTemplateCode(),
        row.getStudentId(),
        row.getEnrollmentId(),
        row.getFileName(),
        row.getContentType(),
        row.getFileSize(),
        row.getStatus(),
        row.getIssuedAt(),
        row.getIssuedByUserId());
  }

  private void assertCanGenerate() {
    if (!hasAuthority("certificate:generate")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "certificate:generate required");
    }
  }

  private void assertCanReadStudent(Long studentId) {
    if (hasAuthority("certificate:read") || hasAuthority("certificate:generate")) {
      return;
    }
    Long self = currentStudentPersonIdOrNull();
    if (self != null && self.equals(studentId)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view certificates");
  }

  private boolean hasAuthority(String code) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(code::equals);
  }

  private Long currentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof ImsUserPrincipal principal) {
      return principal.getUserId();
    }
    return null;
  }

  private Long currentStudentPersonIdOrNull() {
    Long userId = currentUserIdOrNull();
    if (userId == null) {
      return null;
    }
    UserAccount user = userAccountRepository.findById(userId).orElse(null);
    if (user != null
        && "STUDENT".equalsIgnoreCase(user.getPersonType())
        && user.getPersonId() != null) {
      return user.getPersonId();
    }
    return null;
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }
}
