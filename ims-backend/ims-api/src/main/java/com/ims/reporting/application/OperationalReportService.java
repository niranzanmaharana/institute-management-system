package com.ims.reporting.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.AcademicCatalogService.BatchView;
import com.ims.academic.application.AcademicCatalogService.CourseView;
import com.ims.academic.domain.Enrollment;
import com.ims.academic.infrastructure.EnrollmentJpaRepository;
import com.ims.admissions.domain.AdmissionApplication;
import com.ims.admissions.infrastructure.AdmissionApplicationJpaRepository;
import com.ims.common.tenancy.TenantContext;
import com.ims.finance.domain.FeeAdjustment;
import com.ims.finance.domain.StudentFeeAccount;
import com.ims.finance.infrastructure.FeeAdjustmentJpaRepository;
import com.ims.finance.infrastructure.StudentFeeAccountJpaRepository;
import com.ims.operations.api.AttendanceDtos.BatchAttendanceSummary;
import com.ims.operations.api.AttendanceDtos.StudentAttendanceSummary;
import com.ims.operations.api.ExamDtos.ExamResponse;
import com.ims.operations.api.ExamDtos.ExamResultResponse;
import com.ims.operations.application.AttendanceService;
import com.ims.operations.application.ExamService;
import com.ims.people.student.domain.Student;
import com.ims.people.student.infrastructure.StudentJpaRepository;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import com.ims.reporting.api.ReportDtos.ActiveCountRow;
import com.ims.reporting.api.ReportDtos.AdjustmentReportRow;
import com.ims.reporting.api.ReportDtos.AdmissionReportRow;
import com.ims.reporting.api.ReportDtos.BatchRosterRow;
import com.ims.reporting.api.ReportDtos.ExamResultReportRow;
import com.ims.reporting.api.ReportDtos.LowAttendanceReport;
import com.ims.reporting.api.ReportDtos.LowAttendanceRow;
import com.ims.reporting.api.ReportDtos.StudentListRow;
import com.ims.reporting.api.ReportDtos.WithdrawalReportRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationalReportService {

  private static final int MAX_ROWS = 5000;

  private final EnrollmentJpaRepository enrollmentJpaRepository;
  private final StudentJpaRepository studentJpaRepository;
  private final AdmissionApplicationJpaRepository admissionApplicationJpaRepository;
  private final FeeAdjustmentJpaRepository feeAdjustmentJpaRepository;
  private final StudentFeeAccountJpaRepository studentFeeAccountJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final AttendanceService attendanceService;
  private final ExamService examService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public OperationalReportService(
      EnrollmentJpaRepository enrollmentJpaRepository,
      StudentJpaRepository studentJpaRepository,
      AdmissionApplicationJpaRepository admissionApplicationJpaRepository,
      FeeAdjustmentJpaRepository feeAdjustmentJpaRepository,
      StudentFeeAccountJpaRepository studentFeeAccountJpaRepository,
      AcademicCatalogService academicCatalogService,
      AttendanceService attendanceService,
      ExamService examService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.enrollmentJpaRepository = enrollmentJpaRepository;
    this.studentJpaRepository = studentJpaRepository;
    this.admissionApplicationJpaRepository = admissionApplicationJpaRepository;
    this.feeAdjustmentJpaRepository = feeAdjustmentJpaRepository;
    this.studentFeeAccountJpaRepository = studentFeeAccountJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.attendanceService = attendanceService;
    this.examService = examService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  /** RP-ST-01 — one row per matching enrollment (or student with no enrollment if unfiltered). */
  @Transactional(readOnly = true)
  public List<StudentListRow> studentList(String status, Long batchId, Long courseId, String q) {
    long instituteId = requireTenant();
    List<Enrollment> enrollments = loadEnrollments(instituteId, batchId, courseId);
    Map<Long, Student> students = loadStudents(instituteId, enrollments, status, q, batchId, courseId);
    Map<Long, BatchView> batches =
        academicCatalogService.listBatches(null).stream()
            .collect(Collectors.toMap(BatchView::id, Function.identity(), (a, b) -> a));
    Map<Long, CourseView> courses =
        courseMap(enrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toSet()));

    String statusFilter = normalizeOptional(status);
    String qNorm = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

    List<StudentListRow> rows = new ArrayList<>();
    Set<Long> seenStudents = new HashSet<>();
    for (Enrollment e : enrollments) {
      Student student = students.get(e.getStudentId());
      if (student == null) {
        continue;
      }
      if (statusFilter != null && !statusFilter.equalsIgnoreCase(student.getStatus())) {
        continue;
      }
      if (!matchesQuery(student, qNorm)) {
        continue;
      }
      BatchView batch = batches.get(e.getBatchId());
      CourseView course = courses.get(e.getCourseId());
      rows.add(
          new StudentListRow(
              student.getId(),
              student.getStudentCode(),
              fullName(student),
              student.getStatus(),
              student.getPhone(),
              student.getEmail(),
              e.getBatchId(),
              batch == null ? null : batch.code(),
              e.getCourseId(),
              course == null ? null : course.code(),
              e.getStatus()));
      seenStudents.add(student.getId());
    }

    // Unfiltered / status-only: include students without enrollments
    if (batchId == null && courseId == null) {
      for (Student student : students.values()) {
        if (seenStudents.contains(student.getId())) {
          continue;
        }
        if (statusFilter != null && !statusFilter.equalsIgnoreCase(student.getStatus())) {
          continue;
        }
        if (!matchesQuery(student, qNorm)) {
          continue;
        }
        rows.add(
            new StudentListRow(
                student.getId(),
                student.getStudentCode(),
                fullName(student),
                student.getStatus(),
                student.getPhone(),
                student.getEmail(),
                null,
                null,
                null,
                null,
                null));
      }
    }

    rows.sort(
        Comparator.comparing(StudentListRow::studentCode, Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(r -> r.batchCode() == null ? "" : r.batchCode()));
    if (rows.size() > MAX_ROWS) {
      return rows.subList(0, MAX_ROWS);
    }
    return rows;
  }

  /** RP-ST-02 */
  @Transactional(readOnly = true)
  public List<ActiveCountRow> activeCounts(Long courseId) {
    requireTenant();
    List<BatchView> batches = academicCatalogService.listBatches(courseId);
    Map<Long, CourseView> courses =
        courseMap(batches.stream().map(BatchView::courseId).collect(Collectors.toSet()));
    List<ActiveCountRow> rows = new ArrayList<>();
    for (BatchView batch : batches) {
      CourseView course = courses.get(batch.courseId());
      long count =
          enrollmentJpaRepository.countByInstituteIdAndBatchIdAndStatus(
              TenantContext.requireInstituteId(), batch.id(), Enrollment.STATUS_ACTIVE);
      rows.add(
          new ActiveCountRow(
              batch.id(),
              batch.code(),
              batch.name(),
              batch.courseId(),
              course == null ? null : course.code(),
              course == null ? null : course.name(),
              count));
    }
    return rows;
  }

  /** RP-ST-03 */
  @Transactional(readOnly = true)
  public List<AdmissionReportRow> admissions(LocalDate from, LocalDate to, String status) {
    long instituteId = requireTenant();
    InstantRange range = requireDateRange(from, to);
    String statusFilter = normalizeOptional(status);
    List<AdmissionApplication> apps =
        admissionApplicationJpaRepository.findInDateRange(
            instituteId, range.fromInclusive(), range.toExclusive(), statusFilter);
    Map<Long, CourseView> courses =
        courseMap(apps.stream().map(AdmissionApplication::getCourseId).collect(Collectors.toSet()));
    List<AdmissionReportRow> rows = new ArrayList<>();
    for (AdmissionApplication a : apps) {
      if (rows.size() >= MAX_ROWS) {
        break;
      }
      CourseView course = courses.get(a.getCourseId());
      rows.add(
          new AdmissionReportRow(
              a.getId(),
              a.getApplicationNo(),
              a.getApplicantName(),
              a.getCourseId(),
              course == null ? null : course.code(),
              a.getStatus(),
              a.getPhone(),
              a.getEmail(),
              a.getStudentId(),
              a.getCreatedAt(),
              a.getDecidedAt()));
    }
    return rows;
  }

  /** RP-FI-07 — discounts / scholarships / waivers */
  @Transactional(readOnly = true)
  public List<AdjustmentReportRow> adjustments(LocalDate from, LocalDate to, String type) {
    long instituteId = requireTenant();
    InstantRange range = requireDateRange(from, to);
    List<String> types;
    if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type.trim())) {
      types =
          List.of(
              FeeAdjustment.TYPE_DISCOUNT,
              FeeAdjustment.TYPE_SCHOLARSHIP,
              FeeAdjustment.TYPE_WAIVER);
    } else {
      String normalized;
      try {
        normalized = FeeAdjustment.normalizeType(type);
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
      }
      if (!FeeAdjustment.isCredit(normalized)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "type must be DISCOUNT, SCHOLARSHIP, WAIVER, or ALL");
      }
      types = List.of(normalized);
    }
    List<FeeAdjustment> adjustments =
        feeAdjustmentJpaRepository.findCreditsInRange(
            instituteId, types, range.fromInclusive(), range.toExclusive());
    Set<Long> accountIds =
        adjustments.stream().map(FeeAdjustment::getFeeAccountId).collect(Collectors.toSet());
    Map<Long, StudentFeeAccount> accounts = new HashMap<>();
    for (Long id : accountIds) {
      studentFeeAccountJpaRepository
          .findByIdAndInstituteId(id, instituteId)
          .ifPresent(a -> accounts.put(a.getId(), a));
    }
    Set<Long> studentIds =
        accounts.values().stream().map(StudentFeeAccount::getStudentId).collect(Collectors.toSet());
    Map<Long, Student> students =
        studentJpaRepository.findAllById(studentIds).stream()
            .filter(s -> Objects.equals(s.getInstituteId(), instituteId))
            .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));

    List<AdjustmentReportRow> rows = new ArrayList<>();
    for (FeeAdjustment adj : adjustments) {
      if (rows.size() >= MAX_ROWS) {
        break;
      }
      StudentFeeAccount account = accounts.get(adj.getFeeAccountId());
      Student student = account == null ? null : students.get(account.getStudentId());
      rows.add(
          new AdjustmentReportRow(
              adj.getId(),
              adj.getFeeAccountId(),
              account == null ? null : account.getStudentId(),
              student == null ? null : student.getStudentCode(),
              student == null ? null : fullName(student),
              account == null ? null : account.getEnrollmentId(),
              adj.getType(),
              adj.getAmount(),
              adj.getReason(),
              adj.getCreatedAt()));
    }
    return rows;
  }

  /** RP-AT-03 */
  @Transactional(readOnly = true)
  public LowAttendanceReport lowAttendance(Long batchId, BigDecimal thresholdPercent) {
    requireTenant();
    if (batchId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId is required");
    }
    BatchAttendanceSummary summary = attendanceService.batchSummary(batchId);
    BigDecimal threshold = thresholdPercent;
    if (threshold == null) {
      threshold =
          summary.students().isEmpty()
              ? BigDecimal.valueOf(75)
              : summary.students().getFirst().minAttendancePercent();
    }
    Map<Long, Student> students =
        studentJpaRepository
            .findAllById(summary.students().stream().map(StudentAttendanceSummary::studentId).toList())
            .stream()
            .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));

    BigDecimal finalThreshold = threshold;
    List<LowAttendanceRow> rows = new ArrayList<>();
    for (StudentAttendanceSummary s : summary.students()) {
      if (s.sessionsCounted() <= 0) {
        continue;
      }
      if (s.percentage().compareTo(finalThreshold) >= 0) {
        continue;
      }
      Student student = students.get(s.studentId());
      rows.add(
          new LowAttendanceRow(
              s.studentId(),
              student == null ? null : student.getStudentCode(),
              student == null ? null : fullName(student),
              s.batchId(),
              s.presentEffective(),
              s.absent(),
              s.sessionsCounted(),
              s.percentage(),
              finalThreshold));
    }
    rows.sort(Comparator.comparing(LowAttendanceRow::percentage));
    return new LowAttendanceReport(batchId, finalThreshold, rows);
  }

  /** RP-ST-04 — withdrawals / dropouts in date range (by withdrawnAt). */
  @Transactional(readOnly = true)
  public List<WithdrawalReportRow> withdrawals(
      LocalDate from, LocalDate to, Long batchId, Long courseId) {
    long instituteId = requireTenant();
    InstantRange range = requireDateRange(from, to);
    if (batchId != null) {
      academicCatalogService.getBatch(batchId);
    }
    if (courseId != null) {
      academicCatalogService.getCourse(courseId);
    }
    List<Enrollment> enrollments =
        enrollmentJpaRepository
            .findByInstituteIdAndStatusAndWithdrawnAtGreaterThanEqualAndWithdrawnAtLessThanOrderByWithdrawnAtDesc(
                instituteId,
                Enrollment.STATUS_WITHDRAWN,
                range.fromInclusive(),
                range.toExclusive());
    Map<Long, Student> students =
        studentJpaRepository
            .findAllById(enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toSet()))
            .stream()
            .filter(s -> Objects.equals(s.getInstituteId(), instituteId))
            .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));
    Map<Long, BatchView> batches =
        academicCatalogService.listBatches(null).stream()
            .collect(Collectors.toMap(BatchView::id, Function.identity(), (a, b) -> a));
    Map<Long, CourseView> courses =
        courseMap(enrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toSet()));

    List<WithdrawalReportRow> rows = new ArrayList<>();
    for (Enrollment e : enrollments) {
      if (batchId != null && !batchId.equals(e.getBatchId())) {
        continue;
      }
      if (courseId != null && !courseId.equals(e.getCourseId())) {
        continue;
      }
      if (rows.size() >= MAX_ROWS) {
        break;
      }
      Student student = students.get(e.getStudentId());
      BatchView batch = batches.get(e.getBatchId());
      CourseView course = courses.get(e.getCourseId());
      rows.add(
          new WithdrawalReportRow(
              e.getId(),
              e.getStudentId(),
              student == null ? null : student.getStudentCode(),
              student == null ? null : fullName(student),
              e.getBatchId(),
              batch == null ? null : batch.code(),
              e.getCourseId(),
              course == null ? null : course.code(),
              e.getStatus(),
              e.getWithdrawnAt(),
              e.getActivatedAt()));
    }
    return rows;
  }

  /** RP-ST-05 — batch-wise roster (default ACTIVE enrollments). */
  @Transactional(readOnly = true)
  public List<BatchRosterRow> batchRoster(Long batchId, String status) {
    long instituteId = requireTenant();
    if (batchId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId is required");
    }
    BatchView batch = academicCatalogService.getBatch(batchId);
    CourseView course = academicCatalogService.getCourse(batch.courseId());
    String statusFilter = normalizeOptional(status);
    if (statusFilter == null) {
      statusFilter = Enrollment.STATUS_ACTIVE;
    }
    List<Enrollment> enrollments =
        enrollmentJpaRepository.findByInstituteIdAndBatchIdAndStatusOrderByActivatedAtAscIdAsc(
            instituteId, batchId, statusFilter);
    Map<Long, Student> students =
        studentJpaRepository
            .findAllById(enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toSet()))
            .stream()
            .filter(s -> Objects.equals(s.getInstituteId(), instituteId) && s.getDeletedAt() == null)
            .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));

    List<BatchRosterRow> rows = new ArrayList<>();
    for (Enrollment e : enrollments) {
      if (rows.size() >= MAX_ROWS) {
        break;
      }
      Student student = students.get(e.getStudentId());
      if (student == null) {
        continue;
      }
      rows.add(
          new BatchRosterRow(
              e.getId(),
              student.getId(),
              student.getStudentCode(),
              fullName(student),
              student.getPhone(),
              student.getEmail(),
              batch.id(),
              batch.code(),
              batch.name(),
              batch.courseId(),
              course == null ? null : course.code(),
              e.getStatus(),
              e.getActivatedAt()));
    }
    return rows;
  }

  /** RP-EX-01 flattened for export */
  @Transactional(readOnly = true)
  public List<ExamResultReportRow> examResults(Long batchId, Long examId) {
    requireTenant();
    if (batchId == null || examId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId and examId are required");
    }
    ExamResponse exam = examService.get(examId);
    if (!exam.batchId().equals(batchId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam does not belong to batch");
    }
    List<ExamResultResponse> results = examService.batchSummary(batchId, examId).results();
    List<ExamResultReportRow> rows = new ArrayList<>();
    for (ExamResultResponse r : results) {
      rows.add(
          new ExamResultReportRow(
              exam.id(),
              exam.name(),
              exam.status(),
              exam.examDate(),
              r.studentId(),
              r.studentCode(),
              r.studentName(),
              r.resultStatus(),
              r.totalMarks(),
              r.passFail(),
              r.grade()));
    }
    return rows;
  }

  private List<Enrollment> loadEnrollments(long instituteId, Long batchId, Long courseId) {
    if (batchId != null) {
      academicCatalogService.getBatch(batchId);
      return enrollmentJpaRepository.findByInstituteIdAndBatchIdOrderByCreatedAtDesc(
          instituteId, batchId);
    }
    if (courseId != null) {
      academicCatalogService.getCourse(courseId);
      return enrollmentJpaRepository.findByInstituteIdAndCourseIdOrderByCreatedAtDesc(
          instituteId, courseId);
    }
    return enrollmentJpaRepository.findByInstituteIdOrderByCreatedAtDesc(instituteId);
  }

  private Map<Long, Student> loadStudents(
      long instituteId,
      List<Enrollment> enrollments,
      String status,
      String q,
      Long batchId,
      Long courseId) {
    if (batchId != null || courseId != null) {
      Set<Long> ids =
          enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toCollection(HashSet::new));
      return studentJpaRepository.findAllById(ids).stream()
          .filter(s -> Objects.equals(s.getInstituteId(), instituteId) && s.getDeletedAt() == null)
          .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }
    // Broad list: pull active/inactive via search page chunks is heavy — load by enrollment students
    // plus a light all-active scan capped via enrollments + unmatched handled separately using search.
    Set<Long> ids =
        enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toCollection(HashSet::new));
    Map<Long, Student> map =
        studentJpaRepository.findAllById(ids).stream()
            .filter(s -> Objects.equals(s.getInstituteId(), instituteId))
            .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    // Include non-enrolled students matching filters (page through search)
    String statusFilter = normalizeOptional(status);
    var page =
        studentJpaRepository.search(
            instituteId,
            q,
            statusFilter,
            org.springframework.data.domain.PageRequest.of(0, MAX_ROWS));
    for (Student s : page.getContent()) {
      map.putIfAbsent(s.getId(), s);
    }
    return map;
  }

  private Map<Long, CourseView> courseMap(Set<Long> courseIds) {
    Map<Long, CourseView> map = new HashMap<>();
    for (Long id : courseIds) {
      if (id == null) {
        continue;
      }
      map.put(id, academicCatalogService.getCourse(id));
    }
    return map;
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private static InstantRange requireDateRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to dates are required");
    }
    if (to.isBefore(from)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be on or after from");
    }
    if (from.plusDays(366).isBefore(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range cannot exceed 366 days");
    }
    Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return new InstantRange(fromInclusive, toExclusive);
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) {
      return null;
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean matchesQuery(Student student, String qNorm) {
    if (qNorm == null || qNorm.isBlank()) {
      return true;
    }
    return contains(student.getStudentCode(), qNorm)
        || contains(student.getFirstName(), qNorm)
        || contains(student.getLastName(), qNorm)
        || contains(student.getPhone(), qNorm)
        || contains(student.getEmail(), qNorm);
  }

  private static boolean contains(String value, String qNorm) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(qNorm);
  }

  private static String fullName(Student student) {
    String first = student.getFirstName() == null ? "" : student.getFirstName().trim();
    String last = student.getLastName() == null ? "" : student.getLastName().trim();
    return (first + " " + last).trim();
  }

  private record InstantRange(Instant fromInclusive, Instant toExclusive) {}
}
