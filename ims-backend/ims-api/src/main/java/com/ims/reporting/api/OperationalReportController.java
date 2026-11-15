package com.ims.reporting.api;

import com.ims.reporting.api.ReportDtos.ActiveCountRow;
import com.ims.reporting.api.ReportDtos.AdjustmentReportRow;
import com.ims.reporting.api.ReportDtos.AdmissionReportRow;
import com.ims.reporting.api.ReportDtos.BatchRosterRow;
import com.ims.reporting.api.ReportDtos.ExamResultReportRow;
import com.ims.reporting.api.ReportDtos.LowAttendanceReport;
import com.ims.reporting.api.ReportDtos.StudentListRow;
import com.ims.reporting.api.ReportDtos.WithdrawalReportRow;
import com.ims.reporting.application.OperationalReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Operational reports", description = "★ report screens / CSV sources (FR-RP-01)")
@SecurityRequirement(name = "bearer-jwt")
public class OperationalReportController {

  private final OperationalReportService operationalReportService;

  public OperationalReportController(OperationalReportService operationalReportService) {
    this.operationalReportService = operationalReportService;
  }

  @GetMapping("/students")
  @Operation(summary = "Student list with status/batch/course filters (RP-ST-01)")
  @PreAuthorize("hasAuthority('student:read')")
  public List<StudentListRow> students(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long batchId,
      @RequestParam(required = false) Long courseId,
      @RequestParam(required = false) String q) {
    return operationalReportService.studentList(status, batchId, courseId, q);
  }

  @GetMapping("/students/active-counts")
  @Operation(summary = "Active students count by batch/course (RP-ST-02)")
  @PreAuthorize("hasAuthority('student:read') or hasAuthority('course:read')")
  public List<ActiveCountRow> activeCounts(@RequestParam(required = false) Long courseId) {
    return operationalReportService.activeCounts(courseId);
  }

  @GetMapping("/students/withdrawals")
  @Operation(summary = "Withdrawals / dropouts in date range (RP-ST-04)")
  @PreAuthorize("hasAuthority('student:read') or hasAuthority('enrollment:write')")
  public List<WithdrawalReportRow> withdrawals(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long batchId,
      @RequestParam(required = false) Long courseId) {
    return operationalReportService.withdrawals(from, to, batchId, courseId);
  }

  @GetMapping("/students/roster")
  @Operation(summary = "Batch-wise roster (RP-ST-05)")
  @PreAuthorize("hasAuthority('student:read') or hasAuthority('enrollment:write')")
  public List<BatchRosterRow> roster(
      @RequestParam Long batchId, @RequestParam(required = false) String status) {
    return operationalReportService.batchRoster(batchId, status);
  }

  @GetMapping("/admissions")
  @Operation(summary = "New admissions in date range (RP-ST-03)")
  @PreAuthorize(
      "hasAuthority('admission:write') or hasAuthority('admission:approve') or hasAuthority('student:read')")
  public List<AdmissionReportRow> admissions(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String status) {
    return operationalReportService.admissions(from, to, status);
  }

  @GetMapping("/adjustments")
  @Operation(summary = "Discounts / scholarships / waivers (RP-FI-07)")
  @PreAuthorize("hasAuthority('fee:read')")
  public List<AdjustmentReportRow> adjustments(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String type) {
    return operationalReportService.adjustments(from, to, type);
  }

  @GetMapping("/attendance/low")
  @Operation(summary = "Low attendance below threshold (RP-AT-03)")
  @PreAuthorize("hasAuthority('attendance:mark') or hasAuthority('attendance:unlock')")
  public LowAttendanceReport lowAttendance(
      @RequestParam Long batchId, @RequestParam(required = false) BigDecimal thresholdPercent) {
    return operationalReportService.lowAttendance(batchId, thresholdPercent);
  }

  @GetMapping("/exams/results")
  @Operation(summary = "Exam results by batch (RP-EX-01)")
  @PreAuthorize("hasAuthority('exam:mark') or hasAuthority('exam:publish')")
  public List<ExamResultReportRow> examResults(
      @RequestParam Long batchId, @RequestParam Long examId) {
    return operationalReportService.examResults(batchId, examId);
  }
}
