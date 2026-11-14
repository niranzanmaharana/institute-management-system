package com.ims.operations.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.BatchFacultyService;
import com.ims.academic.application.EnrollmentService;
import com.ims.academic.application.EnrollmentService.AttendanceEligibleEnrollment;
import com.ims.common.tenancy.TenantContext;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.operations.api.AttendanceDtos.AttendanceRecordResponse;
import com.ims.operations.api.AttendanceDtos.AttendanceSessionResponse;
import com.ims.operations.api.AttendanceDtos.BatchAttendanceSummary;
import com.ims.operations.api.AttendanceDtos.StudentAttendanceSummary;
import com.ims.operations.domain.AttendanceRecord;
import com.ims.operations.domain.AttendanceSession;
import com.ims.operations.infrastructure.AttendanceRecordJpaRepository;
import com.ims.operations.infrastructure.AttendanceSessionJpaRepository;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class AttendanceService {

  private final AttendanceSessionJpaRepository sessionJpaRepository;
  private final AttendanceRecordJpaRepository recordJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final EnrollmentService enrollmentService;
  private final BatchFacultyService batchFacultyService;
  private final StudentService studentService;
  private final UserAccountRepository userAccountRepository;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public AttendanceService(
      AttendanceSessionJpaRepository sessionJpaRepository,
      AttendanceRecordJpaRepository recordJpaRepository,
      AcademicCatalogService academicCatalogService,
      EnrollmentService enrollmentService,
      BatchFacultyService batchFacultyService,
      StudentService studentService,
      UserAccountRepository userAccountRepository,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.sessionJpaRepository = sessionJpaRepository;
    this.recordJpaRepository = recordJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.enrollmentService = enrollmentService;
    this.batchFacultyService = batchFacultyService;
    this.studentService = studentService;
    this.userAccountRepository = userAccountRepository;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record CreateSessionCommand(Long batchId, LocalDate sessionDate, Long subjectId) {}

  public record MarkRecordCommand(Long studentId, String status, String note) {}

  @Transactional
  public AttendanceSessionResponse create(CreateSessionCommand command) {
    long instituteId = requireTenant();
    var batch = academicCatalogService.getBatch(command.batchId());
    assertCanMarkBatch(batch.id(), command.subjectId());
    if (command.sessionDate() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionDate is required");
    }
    boolean exists =
        command.subjectId() == null
            ? sessionJpaRepository.existsByInstituteIdAndBatchIdAndSessionDateAndSubjectIdIsNull(
                instituteId, command.batchId(), command.sessionDate())
            : sessionJpaRepository.existsByInstituteIdAndBatchIdAndSessionDateAndSubjectId(
                instituteId, command.batchId(), command.sessionDate(), command.subjectId());
    if (exists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance session already exists");
    }
    if (command.subjectId() != null) {
      // ensure subject belongs to course via faculty service list or catalog — validate via assign helper
      academicCatalogService.getCourse(batch.courseId());
    }
    Long userId = currentUserId();
    AttendanceSession saved =
        sessionJpaRepository.save(
            AttendanceSession.create(
                instituteId, command.batchId(), command.sessionDate(), command.subjectId(), userId));
    auditService.record(
        instituteId,
        "operations",
        "AttendanceSession",
        saved.getId(),
        "ATTENDANCE_SESSION_CREATED",
        null,
        Map.of(
            "batchId",
            saved.getBatchId(),
            "sessionDate",
            saved.getSessionDate().toString(),
            "subjectId",
            saved.getSubjectId() == null ? "" : saved.getSubjectId()),
        null);
    return toSessionResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<AttendanceSessionResponse> list(Long batchId, LocalDate from, LocalDate to) {
    long instituteId = requireTenant();
    List<AttendanceSession> rows;
    if (batchId != null) {
      academicCatalogService.getBatch(batchId);
      assertCanViewBatch(batchId, null);
      if (from != null && to != null) {
        rows =
            sessionJpaRepository
                .findByInstituteIdAndBatchIdAndSessionDateBetweenOrderBySessionDateDescIdDesc(
                    instituteId, batchId, from, to);
      } else {
        rows =
            sessionJpaRepository.findByInstituteIdAndBatchIdOrderBySessionDateDescIdDesc(
                instituteId, batchId);
      }
    } else {
      rows = sessionJpaRepository.findByInstituteIdOrderBySessionDateDescIdDesc(instituteId);
      if (!hasAdminMark()) {
        Long facultyId = currentFacultyPersonIdOrNull();
        if (facultyId != null) {
          rows =
              rows.stream()
                  .filter(
                      s ->
                          batchFacultyService.canMarkAttendance(
                              s.getBatchId(), facultyId, s.getSubjectId()))
                  .toList();
        }
      }
    }
    return rows.stream().map(this::toSessionSummary).toList();
  }

  @Transactional(readOnly = true)
  public AttendanceSessionResponse get(Long id) {
    AttendanceSession session = requireSession(id);
    assertCanViewBatch(session.getBatchId(), session.getSubjectId());
    return toSessionResponse(session);
  }

  @Transactional
  public AttendanceSessionResponse mark(Long sessionId, List<MarkRecordCommand> marks) {
    AttendanceSession session = requireSession(sessionId);
    session.requireEditable();
    assertCanEditSession(session);
    if (marks == null || marks.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one mark is required");
    }
    Set<Long> eligible =
        enrollmentService.listAttendanceEligible(session.getBatchId(), session.getSessionDate())
            .stream()
            .map(AttendanceEligibleEnrollment::studentId)
            .collect(Collectors.toCollection(HashSet::new));
    long instituteId = session.getInstituteId();
    for (MarkRecordCommand mark : marks) {
      if (!eligible.contains(mark.studentId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Student " + mark.studentId() + " is not eligible for this session");
      }
      AttendanceRecord row =
          recordJpaRepository
              .findByInstituteIdAndSessionIdAndStudentId(instituteId, sessionId, mark.studentId())
              .orElse(null);
      if (row == null) {
        recordJpaRepository.save(
            AttendanceRecord.create(
                instituteId, sessionId, mark.studentId(), mark.status(), mark.note()));
      } else {
        row.update(mark.status(), mark.note());
        recordJpaRepository.save(row);
      }
    }
    session.touchMarkedBy(currentUserId());
    sessionJpaRepository.save(session);
    auditService.record(
        instituteId,
        "operations",
        "AttendanceSession",
        sessionId,
        "ATTENDANCE_MARKED",
        null,
        Map.of("count", marks.size(), "status", session.getStatus()),
        null);
    return toSessionResponse(session);
  }

  @Transactional
  public AttendanceSessionResponse submit(Long sessionId) {
    AttendanceSession session = requireSession(sessionId);
    session.requireEditable();
    assertCanEditSession(session);
    List<AttendanceEligibleEnrollment> eligible =
        enrollmentService.listAttendanceEligible(session.getBatchId(), session.getSessionDate());
    Map<Long, AttendanceRecord> existing =
        recordJpaRepository
            .findByInstituteIdAndSessionIdOrderByStudentIdAsc(
                session.getInstituteId(), sessionId)
            .stream()
            .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r));
    for (AttendanceEligibleEnrollment e : eligible) {
      if (!existing.containsKey(e.studentId())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "All eligible students must be marked before submit (missing student "
                + e.studentId()
                + ")");
      }
    }
    String before = session.getStatus();
    session.submit();
    sessionJpaRepository.save(session);
    auditService.record(
        session.getInstituteId(),
        "operations",
        "AttendanceSession",
        sessionId,
        "ATTENDANCE_SUBMITTED",
        Map.of("status", before),
        Map.of("status", session.getStatus()),
        null);
    return toSessionResponse(session);
  }

  @Transactional
  public AttendanceSessionResponse lock(Long sessionId) {
    AttendanceSession session = requireSession(sessionId);
    assertCanEditSession(session);
    String before = session.getStatus();
    session.lock();
    sessionJpaRepository.save(session);
    auditService.record(
        session.getInstituteId(),
        "operations",
        "AttendanceSession",
        sessionId,
        "ATTENDANCE_LOCKED",
        Map.of("status", before),
        Map.of("status", session.getStatus()),
        null);
    return toSessionResponse(session);
  }

  @Transactional
  public AttendanceSessionResponse unlock(Long sessionId, String reason) {
    if (reason == null || reason.trim().length() < 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unlock reason min 5 characters");
    }
    AttendanceSession session = requireSession(sessionId);
    if (!hasAuthority("attendance:unlock")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "attendance:unlock required");
    }
    String before = session.getStatus();
    session.unlock();
    sessionJpaRepository.save(session);
    auditService.record(
        session.getInstituteId(),
        "operations",
        "AttendanceSession",
        sessionId,
        "ATTENDANCE_UNLOCKED",
        Map.of("status", before),
        Map.of("status", session.getStatus()),
        reason.trim());
    return toSessionResponse(session);
  }

  @Transactional(readOnly = true)
  public StudentAttendanceSummary studentSummary(Long studentId, Long batchId) {
    long instituteId = requireTenant();
    academicCatalogService.getBatch(batchId);
    assertCanViewBatch(batchId, null);
    assertCanViewStudent(studentId);
    List<AttendanceSession> sessions =
        sessionJpaRepository.findByInstituteIdAndBatchIdOrderBySessionDateDescIdDesc(
            instituteId, batchId);
    List<Long> sessionIds = sessions.stream().map(AttendanceSession::getId).toList();
    Map<Long, AttendanceRecord> bySession = new HashMap<>();
    if (!sessionIds.isEmpty()) {
      for (AttendanceRecord row :
          recordJpaRepository.findByInstituteIdAndStudentIdAndSessionIdIn(
              instituteId, studentId, sessionIds)) {
        bySession.put(row.getSessionId(), row);
      }
    }
    long present = 0;
    long absent = 0;
    long excused = 0;
    for (AttendanceSession session : sessions) {
      if (!AttendanceSession.STATUS_SUBMITTED.equals(session.getStatus())
          && !AttendanceSession.STATUS_LOCKED.equals(session.getStatus())) {
        continue;
      }
      AttendanceRecord row = bySession.get(session.getId());
      if (row == null) {
        continue;
      }
      if (AttendanceRecord.isPresentEffective(row.getStatus())) {
        present++;
      } else if (AttendanceRecord.isAbsent(row.getStatus())) {
        absent++;
      } else if (AttendanceRecord.isExcused(row.getStatus())) {
        excused++;
      }
    }
    long denominator = present + absent;
    BigDecimal pct =
        denominator == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(present)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    var batch = academicCatalogService.getBatch(batchId);
    BigDecimal min = academicCatalogService.getMinAttendancePercent(batch.courseId());
    return new StudentAttendanceSummary(
        studentId,
        batchId,
        present,
        absent,
        excused,
        denominator,
        pct,
        min,
        denominator > 0 && pct.compareTo(min) < 0);
  }

  @Transactional(readOnly = true)
  public BatchAttendanceSummary batchSummary(Long batchId) {
    long instituteId = requireTenant();
    var batch = academicCatalogService.getBatch(batchId);
    assertCanViewBatch(batchId, null);
    List<AttendanceSession> sessions =
        sessionJpaRepository.findByInstituteIdAndBatchIdOrderBySessionDateDescIdDesc(
            instituteId, batchId);
    long draft = sessions.stream().filter(s -> AttendanceSession.STATUS_DRAFT.equals(s.getStatus())).count();
    long submitted =
        sessions.stream().filter(s -> AttendanceSession.STATUS_SUBMITTED.equals(s.getStatus())).count();
    long locked =
        sessions.stream().filter(s -> AttendanceSession.STATUS_LOCKED.equals(s.getStatus())).count();
    Set<Long> studentIds = new HashSet<>();
    for (AttendanceEligibleEnrollment e :
        enrollmentService.listAttendanceEligible(batchId, LocalDate.now().plusYears(50))) {
      studentIds.add(e.studentId());
    }
    // also include any historically marked students
    if (!sessions.isEmpty()) {
      List<Long> ids = sessions.stream().map(AttendanceSession::getId).toList();
      recordJpaRepository
          .findByInstituteIdAndSessionIdIn(instituteId, ids)
          .forEach(r -> studentIds.add(r.getStudentId()));
    }
    List<StudentAttendanceSummary> students = new ArrayList<>();
    for (Long studentId : studentIds.stream().sorted().toList()) {
      students.add(studentSummary(studentId, batchId));
    }
    return new BatchAttendanceSummary(
        batch.id(), sessions.size(), draft, submitted, locked, students);
  }

  private AttendanceSessionResponse toSessionSummary(AttendanceSession session) {
    return new AttendanceSessionResponse(
        session.getId(),
        session.getBatchId(),
        session.getSessionDate(),
        session.getSubjectId(),
        session.getStatus(),
        session.getMarkedBy(),
        session.getLockedAt(),
        session.getUnlockedAt(),
        session.getCreatedAt(),
        List.of());
  }

  private AttendanceSessionResponse toSessionResponse(AttendanceSession session) {
    List<AttendanceEligibleEnrollment> eligible =
        enrollmentService.listAttendanceEligible(session.getBatchId(), session.getSessionDate());
    Map<Long, AttendanceRecord> records =
        recordJpaRepository
            .findByInstituteIdAndSessionIdOrderByStudentIdAsc(
                session.getInstituteId(), session.getId())
            .stream()
            .collect(
                Collectors.toMap(
                    AttendanceRecord::getStudentId, r -> r, (a, b) -> a, LinkedHashMap::new));
    Map<Long, AttendanceRecordResponse> byStudent = new LinkedHashMap<>();
    for (AttendanceEligibleEnrollment e : eligible) {
      AttendanceRecord row = records.get(e.studentId());
      byStudent.put(e.studentId(), toRecordResponse(e.studentId(), row, true));
    }
    for (AttendanceRecord row : records.values()) {
      byStudent.putIfAbsent(row.getStudentId(), toRecordResponse(row.getStudentId(), row, false));
    }
    return new AttendanceSessionResponse(
        session.getId(),
        session.getBatchId(),
        session.getSessionDate(),
        session.getSubjectId(),
        session.getStatus(),
        session.getMarkedBy(),
        session.getLockedAt(),
        session.getUnlockedAt(),
        session.getCreatedAt(),
        List.copyOf(byStudent.values()));
  }

  private AttendanceRecordResponse toRecordResponse(
      Long studentId, AttendanceRecord row, boolean eligible) {
    String code = null;
    String name = null;
    try {
      StudentResponse student = studentService.get(studentId);
      code = student.studentCode();
      name = (student.firstName() + " " + student.lastName()).trim();
    } catch (RuntimeException ignored) {
      code = "?";
      name = "Student #" + studentId;
    }
    return new AttendanceRecordResponse(
        row == null ? null : row.getId(),
        studentId,
        code,
        name,
        row == null ? null : row.getStatus(),
        row == null ? null : row.getNote(),
        eligible);
  }

  private void assertCanMarkBatch(Long batchId, Long subjectId) {
    if (hasAdminMark()) {
      return;
    }
    Long facultyId = currentFacultyPersonIdOrNull();
    if (facultyId == null || !batchFacultyService.canMarkAttendance(batchId, facultyId, subjectId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Faculty may mark only assigned batches");
    }
  }

  private void assertCanViewBatch(Long batchId, Long subjectId) {
    if (hasAuthority("attendance:mark") || hasAuthority("attendance:unlock")) {
      if (hasAdminMark() || hasAuthority("attendance:unlock")) {
        return;
      }
      assertCanMarkBatch(batchId, subjectId);
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view attendance");
  }

  private void assertCanEditSession(AttendanceSession session) {
    assertCanMarkBatch(session.getBatchId(), session.getSubjectId());
    if (hasAdminMark()) {
      return;
    }
    Long userId = currentUserId();
    if (session.getMarkedBy() != null && !session.getMarkedBy().equals(userId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Faculty may edit only their own sessions");
    }
  }

  private void assertCanViewStudent(Long studentId) {
    if (hasAuthority("attendance:mark") || hasAuthority("attendance:unlock")) {
      return;
    }
    UserAccount user = currentUserAccount();
    if ("STUDENT".equalsIgnoreCase(user.getPersonType())
        && studentId.equals(user.getPersonId())) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this student");
  }

  private boolean hasAdminMark() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_PLATFORM_ADMIN".equals(a));
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
    UserAccount user = currentUserAccount();
    if ("FACULTY".equalsIgnoreCase(user.getPersonType()) && user.getPersonId() != null) {
      return user.getPersonId();
    }
    return null;
  }

  private UserAccount currentUserAccount() {
    Long userId = currentUserId();
    return userAccountRepository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
  }

  private Long currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof ImsUserPrincipal principal)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
    return principal.getUserId();
  }

  private AttendanceSession requireSession(Long id) {
    long instituteId = requireTenant();
    return sessionJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }
}
