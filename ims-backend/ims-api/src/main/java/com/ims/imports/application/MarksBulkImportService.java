package com.ims.imports.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.imports.api.MarksImportDtos.MarksImportJobResponse;
import com.ims.imports.application.CsvSupport.CsvTable;
import com.ims.imports.domain.ImportJob;
import com.ims.imports.infrastructure.ImportJobJpaRepository;
import com.ims.operations.api.ExamDtos.ExamComponentResponse;
import com.ims.operations.api.ExamDtos.ExamResponse;
import com.ims.operations.application.ExamService;
import com.ims.operations.application.ExamService.ComponentMarkInput;
import com.ims.operations.application.ExamService.UpsertResultCommand;
import com.ims.operations.domain.Exam;
import com.ims.people.student.api.StudentResponse;
import com.ims.people.student.application.StudentService;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarksBulkImportService {

  public static final int MAX_ROWS = 2000;
  public static final long MAX_BYTES = 5L * 1024 * 1024;
  /** Rows at or below this threshold process in-request; larger jobs run @Async. */
  public static final int SYNC_THRESHOLD = 40;

  private static final Set<String> FIXED_HEADERS =
      Set.of("studentcode", "studentid", "resultstatus", "gracemarks", "grade", "remarks");

  private final ImportJobJpaRepository importJobJpaRepository;
  private final ExamService examService;
  private final StudentService studentService;
  private final MarksImportRowWriter rowWriter;
  private final MarksImportAsyncRunner asyncRunner;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final MarksBulkImportService self;

  public MarksBulkImportService(
      ImportJobJpaRepository importJobJpaRepository,
      ExamService examService,
      StudentService studentService,
      MarksImportRowWriter rowWriter,
      MarksImportAsyncRunner asyncRunner,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler,
      @Lazy MarksBulkImportService self) {
    this.importJobJpaRepository = importJobJpaRepository;
    this.examService = examService;
    this.studentService = studentService;
    this.rowWriter = rowWriter;
    this.asyncRunner = asyncRunner;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.self = self;
  }

  @Transactional(readOnly = true)
  public String templateCsv(Long examId) {
    ExamResponse exam = examService.get(examId);
    List<String> headers = new ArrayList<>();
    headers.add("studentCode");
    headers.add("resultStatus");
    headers.add("graceMarks");
    headers.add("grade");
    headers.add("remarks");
    for (ExamComponentResponse c : exam.components()) {
      headers.add(c.code());
    }
    return CsvSupport.toCsv(headers, List.of());
  }

  /**
   * Not {@code @Transactional}: job insert commits via repository TX, then {@link #processJob}
   * runs in its own TX so row failures cannot poison job status.
   */
  public MarksImportJobResponse start(Long examId, MultipartFile file) {
    long instituteId = requireTenant();
    ExamResponse exam = examService.get(examId);
    if (!Exam.STATUS_DRAFT_MARKS.equals(exam.status())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Marks can only be imported while exam is DRAFT_MARKS");
    }
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is required");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file exceeds 5 MB limit");
    }

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file", ex);
    }
    String payload = new String(bytes, StandardCharsets.UTF_8);
    CsvTable table = CsvSupport.parse(new ByteArrayInputStream(bytes), MAX_ROWS);
    if (table.rows().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV has no data rows");
    }

    ImportJob job =
        importJobJpaRepository.save(
            ImportJob.createMarks(
                instituteId,
                examId,
                file.getOriginalFilename(),
                payload,
                currentUserIdOrNull()));

    auditService.record(
        instituteId,
        "imports",
        "ImportJob",
        job.getId(),
        "MARKS_IMPORT_STARTED",
        null,
        Map.of("examId", examId, "rows", table.rows().size()),
        null);

    boolean async = table.rows().size() > SYNC_THRESHOLD;
    if (async) {
      SecurityContext context = SecurityContextHolder.getContext();
      asyncRunner.processAsync(job.getId(), instituteId, context);
      return toResponse(job, true);
    }

    self.processJob(job.getId());
    return self.getJob(job.getId());
  }

  @Transactional(readOnly = true)
  public MarksImportJobResponse getJob(Long jobId) {
    long instituteId = requireTenant();
    ImportJob job =
        importJobJpaRepository
            .findByIdAndInstituteId(jobId, instituteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    if (!ImportJob.TYPE_MARKS.equals(job.getJobType())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
    }
    return toResponse(job, false);
  }

  @Transactional
  public void processJob(Long jobId) {
    long instituteId = requireTenant();
    ImportJob job =
        importJobJpaRepository
            .findByIdAndInstituteId(jobId, instituteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    if (ImportJob.STATUS_COMPLETED.equals(job.getStatus())
        || ImportJob.STATUS_FAILED.equals(job.getStatus())) {
      return;
    }
    job.markRunning();
    importJobJpaRepository.save(job);

    try {
      if (job.getExamId() == null || job.getPayloadCsv() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job payload missing");
      }
      ExamResponse exam = examService.get(job.getExamId());
      if (!Exam.STATUS_DRAFT_MARKS.equals(exam.status())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam is no longer DRAFT_MARKS");
      }
      Map<String, Long> componentByCode =
          exam.components().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.code().trim().toUpperCase(Locale.ROOT),
                      ExamComponentResponse::id,
                      (a, b) -> a));

      CsvTable table =
          CsvSupport.parse(
              new ByteArrayInputStream(job.getPayloadCsv().getBytes(StandardCharsets.UTF_8)),
              MAX_ROWS);

      List<List<String>> errorRows = new ArrayList<>();
      int success = 0;
      int rowNumber = 1;
      for (Map<String, String> row : table.rows()) {
        rowNumber++;
        try {
          UpsertResultCommand command = toCommand(row, componentByCode);
          rowWriter.upsertOne(exam.id(), command);
          success++;
        } catch (ResponseStatusException ex) {
          errorRows.add(
              List.of(
                  String.valueOf(rowNumber),
                  "row",
                  ex.getReason() == null ? ex.getMessage() : ex.getReason(),
                  summarize(row)));
        } catch (RuntimeException ex) {
          errorRows.add(
              List.of(String.valueOf(rowNumber), "row", "Could not upsert marks", summarize(row)));
        }
      }

      String errorCsv =
          errorRows.isEmpty()
              ? ""
              : CsvSupport.toCsv(List.of("rowNumber", "field", "message", "raw"), errorRows);
      job.markCompleted(table.rows().size(), success, errorRows.size(), errorCsv);
      importJobJpaRepository.save(job);
      auditService.record(
          instituteId,
          "imports",
          "ImportJob",
          job.getId(),
          "MARKS_IMPORT_COMPLETED",
          null,
          Map.of("success", success, "failed", errorRows.size()),
          null);
    } catch (RuntimeException ex) {
      String msg =
          ex instanceof ResponseStatusException rse && rse.getReason() != null
              ? rse.getReason()
              : ex.getMessage();
      job.markFailed(msg == null ? "Import failed" : msg);
      importJobJpaRepository.save(job);
      // Do not rethrow — persist FAILED status for job polling.
    }
  }

  private UpsertResultCommand toCommand(Map<String, String> row, Map<String, Long> componentByCode) {
    String studentCode = CsvSupport.cell(row, "studentCode", "studentcode");
    String studentIdRaw = CsvSupport.cell(row, "studentId", "studentid");
    Long studentId;
    if (!studentCode.isBlank()) {
      StudentResponse student = studentService.requireByStudentCode(studentCode);
      studentId = student.id();
    } else if (!studentIdRaw.isBlank()) {
      try {
        studentId = Long.parseLong(studentIdRaw.trim());
      } catch (NumberFormatException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid studentId");
      }
      studentService.get(studentId);
    } else {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "studentCode or studentId is required");
    }

    String resultStatus = blankToNull(CsvSupport.cell(row, "resultStatus", "resultstatus"));
    String grade = blankToNull(CsvSupport.cell(row, "grade"));
    String remarks = blankToNull(CsvSupport.cell(row, "remarks"));
    String graceRaw = CsvSupport.cell(row, "graceMarks", "gracemarks");
    BigDecimal grace = null;
    if (!graceRaw.isBlank()) {
      try {
        grace = new BigDecimal(graceRaw.trim());
      } catch (NumberFormatException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid graceMarks");
      }
    }

    List<ComponentMarkInput> marks = new ArrayList<>();
    for (Map.Entry<String, String> entry : row.entrySet()) {
      String header = entry.getKey();
      if (FIXED_HEADERS.contains(header)) {
        continue;
      }
      Long componentId = componentByCode.get(header.toUpperCase(Locale.ROOT));
      if (componentId == null) {
        continue;
      }
      String raw = entry.getValue() == null ? "" : entry.getValue().trim();
      BigDecimal value = null;
      if (!raw.isBlank()) {
        try {
          value = new BigDecimal(raw);
        } catch (NumberFormatException ex) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Invalid marks for component " + header);
        }
      }
      marks.add(new ComponentMarkInput(componentId, value));
    }

    return new UpsertResultCommand(studentId, resultStatus, grace, grade, remarks, marks);
  }

  private MarksImportJobResponse toResponse(ImportJob job, boolean async) {
    return new MarksImportJobResponse(
        job.getId(),
        job.getExamId(),
        job.getStatus(),
        job.getFileName(),
        job.getTotalRows(),
        job.getSuccessCount(),
        job.getFailedCount(),
        job.getErrorReportCsv() == null ? "" : job.getErrorReportCsv(),
        job.getMessage(),
        async,
        job.getCreatedAt(),
        job.getStartedAt(),
        job.getCompletedAt());
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private Long currentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof ImsUserPrincipal principal) {
      return principal.getUserId();
    }
    return null;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String summarize(Map<String, String> row) {
    return String.join(
        "|",
        row.entrySet().stream()
            .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
            .toList());
  }
}
