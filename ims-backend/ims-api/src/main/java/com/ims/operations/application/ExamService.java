package com.ims.operations.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.BatchFacultyService;
import com.ims.academic.application.EnrollmentService;
import com.ims.academic.application.EnrollmentService.AttendanceEligibleEnrollment;
import com.ims.common.tenancy.TenantContext;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.operations.api.ExamDtos.BatchExamResultsSummary;
import com.ims.operations.api.ExamDtos.ComponentMarkResponse;
import com.ims.operations.api.ExamDtos.ExamComponentResponse;
import com.ims.operations.api.ExamDtos.ExamResponse;
import com.ims.operations.api.ExamDtos.ExamResultResponse;
import com.ims.operations.domain.Exam;
import com.ims.operations.domain.ExamComponent;
import com.ims.operations.domain.ExamComponentMark;
import com.ims.operations.domain.ExamResult;
import com.ims.operations.infrastructure.ExamComponentJpaRepository;
import com.ims.operations.infrastructure.ExamComponentMarkJpaRepository;
import com.ims.operations.infrastructure.ExamJpaRepository;
import com.ims.operations.infrastructure.ExamResultJpaRepository;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamService {

  private static final BigDecimal GRACE_CAP = new BigDecimal("10.00");

  private final ExamJpaRepository examJpaRepository;
  private final ExamComponentJpaRepository componentJpaRepository;
  private final ExamResultJpaRepository resultJpaRepository;
  private final ExamComponentMarkJpaRepository markJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final BatchFacultyService batchFacultyService;
  private final EnrollmentService enrollmentService;
  private final StudentService studentService;
  private final UserAccountRepository userAccountRepository;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public ExamService(
      ExamJpaRepository examJpaRepository,
      ExamComponentJpaRepository componentJpaRepository,
      ExamResultJpaRepository resultJpaRepository,
      ExamComponentMarkJpaRepository markJpaRepository,
      AcademicCatalogService academicCatalogService,
      BatchFacultyService batchFacultyService,
      EnrollmentService enrollmentService,
      StudentService studentService,
      UserAccountRepository userAccountRepository,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.examJpaRepository = examJpaRepository;
    this.componentJpaRepository = componentJpaRepository;
    this.resultJpaRepository = resultJpaRepository;
    this.markJpaRepository = markJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.batchFacultyService = batchFacultyService;
    this.enrollmentService = enrollmentService;
    this.studentService = studentService;
    this.userAccountRepository = userAccountRepository;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record CreateExamCommand(
      Long batchId, String name, LocalDate examDate, Long originalExamId) {}

  public record UpdateExamCommand(String name, LocalDate examDate) {}

  public record CreateComponentCommand(
      Long subjectId,
      String code,
      String name,
      BigDecimal maxMarks,
      BigDecimal passMarks,
      BigDecimal weightage,
      Integer seq) {}

  public record ComponentMarkInput(Long componentId, BigDecimal marks) {}

  public record UpsertResultCommand(
      Long studentId,
      String resultStatus,
      BigDecimal graceMarks,
      String grade,
      String remarks,
      List<ComponentMarkInput> componentMarks) {}

  @Transactional(readOnly = true)
  public List<String> listGrades() {
    return ExamResult.GRADES;
  }

  @Transactional
  public ExamResponse create(CreateExamCommand command) {
    long instituteId = requireTenant();
    academicCatalogService.getBatch(command.batchId());
    assertCanMarkBatch(command.batchId());
    if (command.originalExamId() != null) {
      requireExam(command.originalExamId());
    }
    Exam saved =
        examJpaRepository.save(
            Exam.create(
                instituteId,
                command.batchId(),
                command.name(),
                command.examDate(),
                command.originalExamId()));
    auditService.record(
        instituteId,
        "operations",
        "Exam",
        saved.getId(),
        "EXAM_CREATED",
        null,
        Map.of("batchId", saved.getBatchId(), "name", saved.getName()),
        null);
    return toExamResponse(saved);
  }

  @Transactional
  public ExamResponse update(Long id, UpdateExamCommand command) {
    Exam exam = requireExam(id);
    assertCanMarkBatch(exam.getBatchId());
    exam.update(command.name(), command.examDate());
    return toExamResponse(examJpaRepository.save(exam));
  }

  @Transactional(readOnly = true)
  public List<ExamResponse> list(Long batchId) {
    long instituteId = requireTenant();
    List<Exam> rows;
    if (batchId != null) {
      academicCatalogService.getBatch(batchId);
      assertCanViewBatch(batchId);
      rows = examJpaRepository.findByInstituteIdAndBatchIdOrderByExamDateDescIdDesc(instituteId, batchId);
    } else {
      rows = examJpaRepository.findByInstituteIdOrderByExamDateDescIdDesc(instituteId);
      if (!hasAdminOrCoord() && !hasAuthority("exam:publish")) {
        Long facultyId = currentFacultyPersonIdOrNull();
        if (facultyId != null) {
          rows =
              rows.stream()
                  .filter(e -> batchFacultyService.canMarkAttendance(e.getBatchId(), facultyId, null))
                  .toList();
        }
      }
    }
    return rows.stream().map(this::toExamSummary).toList();
  }

  @Transactional(readOnly = true)
  public ExamResponse get(Long id) {
    Exam exam = requireExam(id);
    assertCanViewBatch(exam.getBatchId());
    return toExamResponse(exam);
  }

  @Transactional
  public ExamComponentResponse addComponent(Long examId, CreateComponentCommand command) {
    Exam exam = requireExam(examId);
    assertCanMarkBatch(exam.getBatchId());
    if (!Exam.STATUS_SCHEDULED.equals(exam.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Components can only be added while SCHEDULED");
    }
    var batch = academicCatalogService.getBatch(exam.getBatchId());
    if (command.subjectId() != null) {
      boolean ok =
          batchFacultyService.listSubjects(batch.courseId()).stream()
              .anyMatch(s -> s.id().equals(command.subjectId()));
      if (!ok) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject not on this course");
      }
    }
    String code = command.code() == null ? "" : command.code().trim().toUpperCase();
    if (componentJpaRepository.existsByExamIdAndCodeIgnoreCase(examId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Component code already exists");
    }
    int seq =
        command.seq() != null
            ? command.seq()
            : (int) componentJpaRepository.countByInstituteIdAndExamId(exam.getInstituteId(), examId)
                + 1;
    ExamComponent saved =
        componentJpaRepository.save(
            ExamComponent.create(
                exam.getInstituteId(),
                examId,
                command.subjectId(),
                command.code(),
                command.name(),
                command.maxMarks(),
                command.passMarks(),
                command.weightage(),
                seq));
    return toComponent(saved);
  }

  @Transactional
  public ExamResponse openMarks(Long examId) {
    Exam exam = requireExam(examId);
    assertCanMarkBatch(exam.getBatchId());
    if (componentJpaRepository.countByInstituteIdAndExamId(exam.getInstituteId(), examId) < 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Add at least one component before opening marks");
    }
    String before = exam.getStatus();
    exam.openMarks();
    examJpaRepository.save(exam);
    seedEligibleResults(exam);
    auditService.record(
        exam.getInstituteId(),
        "operations",
        "Exam",
        examId,
        "EXAM_OPENED_MARKS",
        Map.of("status", before),
        Map.of("status", exam.getStatus()),
        null);
    return toExamResponse(exam);
  }

  private void seedEligibleResults(Exam exam) {
    List<AttendanceEligibleEnrollment> eligible =
        enrollmentService.listAttendanceEligible(exam.getBatchId(), exam.getExamDate());
    for (AttendanceEligibleEnrollment row : eligible) {
      resultJpaRepository
          .findByInstituteIdAndExamIdAndStudentId(
              exam.getInstituteId(), exam.getId(), row.studentId())
          .orElseGet(
              () ->
                  resultJpaRepository.save(
                      ExamResult.create(
                          exam.getInstituteId(), exam.getId(), row.studentId())));
    }
  }

  @Transactional
  public List<ExamResultResponse> upsertResults(Long examId, List<UpsertResultCommand> commands) {
    Exam exam = requireExam(examId);
    exam.requireMarksEditable();
    assertCanMarkBatch(exam.getBatchId());
    if (commands == null || commands.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one result is required");
    }
    List<ExamComponent> components =
        componentJpaRepository.findByInstituteIdAndExamIdOrderBySeqAscIdAsc(
            exam.getInstituteId(), examId);
    Map<Long, ExamComponent> componentById =
        components.stream().collect(Collectors.toMap(ExamComponent::getId, c -> c));
    Set<Long> eligible =
        enrollmentService.listAttendanceEligible(exam.getBatchId(), exam.getExamDate()).stream()
            .map(AttendanceEligibleEnrollment::studentId)
            .collect(Collectors.toCollection(HashSet::new));
    boolean canGrace = hasAuthority("exam:publish") || hasAdminOrCoord();
    List<ExamResultResponse> out = new ArrayList<>();
    for (UpsertResultCommand cmd : commands) {
      if (!eligible.contains(cmd.studentId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Student " + cmd.studentId() + " is not eligible for this exam");
      }
      ExamResult result =
          resultJpaRepository
              .findByInstituteIdAndExamIdAndStudentId(
                  exam.getInstituteId(), examId, cmd.studentId())
              .orElseGet(
                  () ->
                      resultJpaRepository.save(
                          ExamResult.create(exam.getInstituteId(), examId, cmd.studentId())));
      String status =
          cmd.resultStatus() == null ? ExamResult.STATUS_MARKS : cmd.resultStatus().trim().toUpperCase();
      BigDecimal grace = result.getGraceMarks();
      if (cmd.graceMarks() != null) {
        if (!canGrace) {
          throw new ResponseStatusException(
              HttpStatus.FORBIDDEN, "Only ADMIN/COORD can set grace marks");
        }
        if (cmd.graceMarks().compareTo(BigDecimal.ZERO) < 0
            || cmd.graceMarks().compareTo(GRACE_CAP) > 0) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "graceMarks must be between 0 and " + GRACE_CAP);
        }
        grace = cmd.graceMarks();
      }
      BigDecimal total = null;
      String passFail = null;
      if (ExamResult.STATUS_MARKS.equals(status)) {
        Map<Long, BigDecimal> marksByComponent = new HashMap<>();
        if (cmd.componentMarks() != null) {
          for (ComponentMarkInput mark : cmd.componentMarks()) {
            ExamComponent component = componentById.get(mark.componentId());
            if (component == null) {
              throw new ResponseStatusException(
                  HttpStatus.BAD_REQUEST, "Unknown component " + mark.componentId());
            }
            if (mark.marks() != null) {
              if (mark.marks().compareTo(BigDecimal.ZERO) < 0
                  || mark.marks().compareTo(component.getMaxMarks()) > 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Marks for " + component.getCode() + " must be 0.." + component.getMaxMarks());
              }
            }
            ExamComponentMark row =
                markJpaRepository
                    .findByInstituteIdAndExamResultIdAndExamComponentId(
                        exam.getInstituteId(), result.getId(), component.getId())
                    .orElse(null);
            if (row == null) {
              markJpaRepository.save(
                  ExamComponentMark.create(
                      exam.getInstituteId(), result.getId(), component.getId(), mark.marks()));
            } else {
              row.setMarks(mark.marks());
              markJpaRepository.save(row);
            }
            marksByComponent.put(component.getId(), mark.marks());
          }
        }
        // load all marks for total (marks may be null when only partially entered)
        List<ExamComponentMark> allMarks =
            markJpaRepository.findByInstituteIdAndExamResultIdIn(
                exam.getInstituteId(), List.of(result.getId()));
        total = BigDecimal.ZERO;
        BigDecimal passThreshold = BigDecimal.ZERO;
        boolean complete = true;
        for (ExamComponent component : components) {
          passThreshold = passThreshold.add(component.getPassMarks());
          BigDecimal m =
              allMarks.stream()
                  .filter(x -> x.getExamComponentId().equals(component.getId()))
                  .findFirst()
                  .map(ExamComponentMark::getMarks)
                  .orElseGet(() -> marksByComponent.get(component.getId()));
          if (m == null) {
            complete = false;
          } else {
            total = total.add(m);
          }
        }
        if (complete) {
          if (grace != null) {
            total = total.add(grace);
          }
          passFail = total.compareTo(passThreshold) >= 0 ? ExamResult.PASS : ExamResult.FAIL;
        } else {
          // Keep draft totals unset until every component has a mark
          total = null;
          passFail = null;
        }
      } else {
        // ABSENT / WITHHELD — clear numeric outcome
        total = null;
        passFail = null;
        grace = null;
      }
      result.apply(status, total, grace, passFail, cmd.grade(), cmd.remarks());
      resultJpaRepository.save(result);
      out.add(toResultResponse(result, components));
    }
    auditService.record(
        exam.getInstituteId(),
        "operations",
        "Exam",
        examId,
        "EXAM_MARKS_UPSERTED",
        null,
        Map.of("count", commands.size()),
        null);
    return out;
  }

  @Transactional
  public ExamResponse publish(Long examId) {
    Exam exam = requireExam(examId);
    assertCanPublish();
    assertCanViewBatch(exam.getBatchId());
    long results = resultJpaRepository.countByInstituteIdAndExamId(exam.getInstituteId(), examId);
    if (results < 1) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Enter marks before publishing");
    }
    String before = exam.getStatus();
    exam.publish();
    examJpaRepository.save(exam);
    auditService.record(
        exam.getInstituteId(),
        "operations",
        "Exam",
        examId,
        "EXAM_PUBLISHED",
        Map.of("status", before),
        Map.of("status", exam.getStatus()),
        null);
    return toExamResponse(exam);
  }

  @Transactional
  public ExamResponse lock(Long examId) {
    Exam exam = requireExam(examId);
    assertCanPublish();
    String before = exam.getStatus();
    exam.lock();
    examJpaRepository.save(exam);
    auditService.record(
        exam.getInstituteId(),
        "operations",
        "Exam",
        examId,
        "EXAM_LOCKED",
        Map.of("status", before),
        Map.of("status", exam.getStatus()),
        null);
    return toExamResponse(exam);
  }

  @Transactional
  public ExamResponse unlock(Long examId, String reason) {
    if (reason == null || reason.trim().length() < 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unlock reason min 5 characters");
    }
    Exam exam = requireExam(examId);
    assertCanPublish();
    String before = exam.getStatus();
    exam.unlock();
    examJpaRepository.save(exam);
    auditService.record(
        exam.getInstituteId(),
        "operations",
        "Exam",
        examId,
        "EXAM_UNLOCKED",
        Map.of("status", before),
        Map.of("status", exam.getStatus()),
        reason.trim());
    return toExamResponse(exam);
  }

  @Transactional(readOnly = true)
  public List<ExamResultResponse> listResults(Long examId) {
    Exam exam = requireExam(examId);
    assertCanViewResults(exam);
    List<ExamComponent> components =
        componentJpaRepository.findByInstituteIdAndExamIdOrderBySeqAscIdAsc(
            exam.getInstituteId(), examId);
    List<ExamResult> results =
        resultJpaRepository.findByInstituteIdAndExamIdOrderByStudentIdAsc(
            exam.getInstituteId(), examId);
    Long studentSelf = currentStudentPersonIdOrNull();
    if (studentSelf != null && !hasAuthority("exam:mark") && !hasAuthority("exam:publish")) {
      results = results.stream().filter(r -> studentSelf.equals(r.getStudentId())).toList();
    }
    return results.stream().map(r -> toResultResponse(r, components)).toList();
  }

  @Transactional(readOnly = true)
  public BatchExamResultsSummary batchSummary(Long batchId, Long examId) {
    Exam exam = requireExam(examId);
    if (!exam.getBatchId().equals(batchId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam does not belong to batch");
    }
    assertCanViewResults(exam);
    return new BatchExamResultsSummary(
        batchId, examId, exam.getName(), exam.getStatus(), listResults(examId));
  }

  private void assertCanViewResults(Exam exam) {
    Long studentSelf = currentStudentPersonIdOrNull();
    if (studentSelf != null && !hasAuthority("exam:mark") && !hasAuthority("exam:publish")) {
      if (!exam.isVisibleToStudents()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Results not published yet");
      }
      return;
    }
    assertCanViewBatch(exam.getBatchId());
  }

  private ExamResponse toExamSummary(Exam exam) {
    return new ExamResponse(
        exam.getId(),
        exam.getBatchId(),
        exam.getName(),
        exam.getExamDate(),
        exam.getStatus(),
        exam.getOriginalExamId(),
        exam.getPublishedAt(),
        exam.getLockedAt(),
        List.of());
  }

  private ExamResponse toExamResponse(Exam exam) {
    List<ExamComponentResponse> components =
        componentJpaRepository
            .findByInstituteIdAndExamIdOrderBySeqAscIdAsc(exam.getInstituteId(), exam.getId())
            .stream()
            .map(this::toComponent)
            .toList();
    return new ExamResponse(
        exam.getId(),
        exam.getBatchId(),
        exam.getName(),
        exam.getExamDate(),
        exam.getStatus(),
        exam.getOriginalExamId(),
        exam.getPublishedAt(),
        exam.getLockedAt(),
        components);
  }

  private ExamComponentResponse toComponent(ExamComponent c) {
    return new ExamComponentResponse(
        c.getId(),
        c.getSubjectId(),
        c.getCode(),
        c.getName(),
        c.getMaxMarks(),
        c.getPassMarks(),
        c.getWeightage(),
        c.getSeq());
  }

  private ExamResultResponse toResultResponse(ExamResult result, List<ExamComponent> components) {
    String code = null;
    String name = null;
    try {
      StudentResponse student = studentService.get(result.getStudentId());
      code = student.studentCode();
      name = (student.firstName() + " " + student.lastName()).trim();
    } catch (RuntimeException ignored) {
      code = "?";
      name = "Student #" + result.getStudentId();
    }
    Map<Long, BigDecimal> marks =
        markJpaRepository
            .findByInstituteIdAndExamResultIdIn(
                result.getInstituteId(), List.of(result.getId()))
            .stream()
            .collect(
                Collectors.toMap(
                    ExamComponentMark::getExamComponentId,
                    ExamComponentMark::getMarks,
                    (a, b) -> a));
    List<ComponentMarkResponse> componentMarks =
        components.stream()
            .map(
                c ->
                    new ComponentMarkResponse(
                        c.getId(), c.getCode(), marks.get(c.getId())))
            .toList();
    return new ExamResultResponse(
        result.getId(),
        result.getExamId(),
        result.getStudentId(),
        code,
        name,
        result.getResultStatus(),
        result.getTotalMarks(),
        result.getGraceMarks(),
        result.getPassFail(),
        result.getGrade(),
        result.getRemarks(),
        componentMarks);
  }

  private void assertCanMarkBatch(Long batchId) {
    if (hasAdminOrCoord() || hasAuthority("exam:publish")) {
      return;
    }
    if (!hasAuthority("exam:mark")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "exam:mark required");
    }
    Long facultyId = currentFacultyPersonIdOrNull();
    if (facultyId == null || !batchFacultyService.canMarkAttendance(batchId, facultyId, null)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Faculty may mark only assigned batches");
    }
  }

  private void assertCanViewBatch(Long batchId) {
    if (hasAuthority("exam:mark") || hasAuthority("exam:publish") || hasAuthority("course:read")) {
      if (hasAdminOrCoord() || hasAuthority("exam:publish") || hasAuthority("course:read")) {
        return;
      }
      assertCanMarkBatch(batchId);
      return;
    }
    Long studentSelf = currentStudentPersonIdOrNull();
    if (studentSelf != null) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view exams");
  }

  private void assertCanPublish() {
    if (!hasAuthority("exam:publish")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "exam:publish required");
    }
  }

  private boolean hasAdminOrCoord() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(
            a ->
                "ROLE_ADMIN".equals(a)
                    || "ROLE_PLATFORM_ADMIN".equals(a)
                    || "ROLE_ACADEMIC_COORDINATOR".equals(a));
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

  private Long currentFacultyPersonIdOrNull() {
    UserAccount user = currentUserAccountOrNull();
    if (user != null
        && "FACULTY".equalsIgnoreCase(user.getPersonType())
        && user.getPersonId() != null) {
      return user.getPersonId();
    }
    return null;
  }

  private Long currentStudentPersonIdOrNull() {
    UserAccount user = currentUserAccountOrNull();
    if (user != null
        && "STUDENT".equalsIgnoreCase(user.getPersonType())
        && user.getPersonId() != null) {
      return user.getPersonId();
    }
    return null;
  }

  private UserAccount currentUserAccountOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof ImsUserPrincipal principal)) {
      return null;
    }
    return userAccountRepository.findById(principal.getUserId()).orElse(null);
  }

  private Exam requireExam(Long id) {
    long instituteId = requireTenant();
    return examJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }
}
