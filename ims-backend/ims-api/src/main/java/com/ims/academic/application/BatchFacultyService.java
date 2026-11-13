package com.ims.academic.application;

import com.ims.academic.domain.BatchFacultyAssignment;
import com.ims.academic.domain.Subject;
import com.ims.academic.infrastructure.BatchFacultyAssignmentJpaRepository;
import com.ims.academic.infrastructure.SubjectJpaRepository;
import com.ims.people.faculty.application.FacultyService;
import com.ims.people.faculty.application.FacultyService.FacultyRef;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import com.ims.common.tenancy.TenantContext;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchFacultyService {

  private final AcademicCatalogService academicCatalogService;
  private final SubjectJpaRepository subjectJpaRepository;
  private final BatchFacultyAssignmentJpaRepository assignmentJpaRepository;
  private final FacultyService facultyService;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public BatchFacultyService(
      AcademicCatalogService academicCatalogService,
      SubjectJpaRepository subjectJpaRepository,
      BatchFacultyAssignmentJpaRepository assignmentJpaRepository,
      FacultyService facultyService,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.academicCatalogService = academicCatalogService;
    this.subjectJpaRepository = subjectJpaRepository;
    this.assignmentJpaRepository = assignmentJpaRepository;
    this.facultyService = facultyService;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record SubjectView(Long id, Long courseId, String code, String name, String status) {}

  public record FacultyAssignmentView(
      Long id,
      Long batchId,
      Long facultyId,
      String facultyCode,
      String facultyName,
      Long subjectId,
      String subjectCode,
      String subjectName,
      String role) {}

  @Transactional(readOnly = true)
  public List<SubjectView> listSubjects(Long courseId) {
    long instituteId = requireTenant();
    academicCatalogService.getCourse(courseId);
    return subjectJpaRepository
        .findByInstituteIdAndCourseIdOrderByCodeAsc(instituteId, courseId)
        .stream()
        .map(this::toSubject)
        .toList();
  }

  @Transactional
  public SubjectView createSubject(Long courseId, String code, String name) {
    long instituteId = requireTenant();
    academicCatalogService.getCourse(courseId);
    String normalized = resolveSubjectCode(instituteId, courseId, code);
    if (subjectJpaRepository.existsByInstituteIdAndCourseIdAndCodeIgnoreCase(
        instituteId, courseId, normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject code already exists");
    }
    Subject saved =
        subjectJpaRepository.save(Subject.create(instituteId, courseId, normalized, name));
    return toSubject(saved);
  }

  @Transactional(readOnly = true)
  public List<FacultyAssignmentView> listAssignments(Long batchId) {
    long instituteId = requireTenant();
    academicCatalogService.getBatch(batchId);
    return assignmentJpaRepository
        .findByInstituteIdAndBatchIdOrderByCreatedAtDesc(instituteId, batchId)
        .stream()
        .map(this::toAssignment)
        .toList();
  }

  @Transactional
  public FacultyAssignmentView assign(Long batchId, Long facultyId, Long subjectId, String role) {
    long instituteId = requireTenant();
    var batch = academicCatalogService.getBatch(batchId);
    FacultyRef faculty = facultyService.requireActiveNamed(facultyId);
    Long resolvedSubject = subjectId;
    if (resolvedSubject != null) {
      Subject subject =
          subjectJpaRepository
              .findByIdAndInstituteId(resolvedSubject, instituteId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
      if (!subject.getCourseId().equals(batch.courseId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Subject does not belong to this batch's course");
      }
    }
    boolean duplicate =
        resolvedSubject == null
            ? assignmentJpaRepository.existsByInstituteIdAndBatchIdAndFacultyIdAndSubjectIdIsNull(
                instituteId, batchId, facultyId)
            : assignmentJpaRepository.existsByInstituteIdAndBatchIdAndFacultyIdAndSubjectId(
                instituteId, batchId, facultyId, resolvedSubject);
    if (duplicate) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Faculty is already assigned");
    }
    BatchFacultyAssignment saved =
        assignmentJpaRepository.save(
            BatchFacultyAssignment.create(instituteId, batchId, facultyId, resolvedSubject, role));
    auditService.record(
        instituteId,
        "academic",
        "BatchFacultyAssignment",
        saved.getId(),
        "FACULTY_ASSIGNED",
        null,
        Map.of(
            "batchId",
            batchId,
            "facultyId",
            facultyId,
            "facultyCode",
            faculty.facultyCode(),
            "subjectId",
            resolvedSubject == null ? "" : resolvedSubject,
            "role",
            saved.getRole()),
        null);
    return toAssignment(saved);
  }

  @Transactional
  public void remove(Long batchId, Long assignmentId) {
    long instituteId = requireTenant();
    academicCatalogService.getBatch(batchId);
    BatchFacultyAssignment row =
        assignmentJpaRepository
            .findByIdAndInstituteId(assignmentId, instituteId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    if (!row.getBatchId().equals(batchId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
    }
    assignmentJpaRepository.delete(row);
    auditService.record(
        instituteId,
        "academic",
        "BatchFacultyAssignment",
        assignmentId,
        "FACULTY_UNASSIGNED",
        Map.of("batchId", batchId, "facultyId", row.getFacultyId(), "role", row.getRole()),
        null,
        null);
  }

  private String resolveSubjectCode(long instituteId, Long courseId, String code) {
    if (code != null && !code.isBlank()) {
      return code.trim().toUpperCase();
    }
    long n = subjectJpaRepository.countByInstituteIdAndCourseId(instituteId, courseId) + 1;
    return "SUB-" + courseId + "-" + n;
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private SubjectView toSubject(Subject s) {
    return new SubjectView(s.getId(), s.getCourseId(), s.getCode(), s.getName(), s.getStatus());
  }

  private FacultyAssignmentView toAssignment(BatchFacultyAssignment row) {
    FacultyRef faculty =
        facultyService
            .findNamed(row.getFacultyId())
            .orElse(new FacultyRef(row.getFacultyId(), "?", "Unknown", "faculty", "UNKNOWN"));
    String subjectCode = null;
    String subjectName = null;
    if (row.getSubjectId() != null) {
      Subject subject =
          subjectJpaRepository
              .findByIdAndInstituteId(row.getSubjectId(), row.getInstituteId())
              .orElse(null);
      if (subject != null) {
        subjectCode = subject.getCode();
        subjectName = subject.getName();
      }
    }
    return new FacultyAssignmentView(
        row.getId(),
        row.getBatchId(),
        row.getFacultyId(),
        faculty.facultyCode(),
        (faculty.firstName() + " " + faculty.lastName()).trim(),
        row.getSubjectId(),
        subjectCode,
        subjectName,
        row.getRole());
  }
}
