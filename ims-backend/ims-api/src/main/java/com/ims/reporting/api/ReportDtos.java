package com.ims.reporting.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {

  private ReportDtos() {}

  /** RP-ST-01 */
  public record StudentListRow(
      Long studentId,
      String studentCode,
      String fullName,
      String status,
      String phone,
      String email,
      Long batchId,
      String batchCode,
      Long courseId,
      String courseCode,
      String enrollmentStatus) {}

  /** RP-ST-02 */
  public record ActiveCountRow(
      Long batchId,
      String batchCode,
      String batchName,
      Long courseId,
      String courseCode,
      String courseName,
      long activeCount) {}

  /** RP-ST-03 */
  public record AdmissionReportRow(
      Long applicationId,
      String applicationNo,
      String applicantName,
      Long courseId,
      String courseCode,
      String status,
      String phone,
      String email,
      Long studentId,
      Instant createdAt,
      Instant decidedAt) {}

  /** RP-FI-07 */
  public record AdjustmentReportRow(
      Long adjustmentId,
      Long feeAccountId,
      Long studentId,
      String studentCode,
      String studentName,
      Long enrollmentId,
      String type,
      BigDecimal amount,
      String reason,
      Instant createdAt) {}

  /** RP-AT-03 */
  public record LowAttendanceRow(
      Long studentId,
      String studentCode,
      String studentName,
      Long batchId,
      long presentEffective,
      long absent,
      long sessionsCounted,
      BigDecimal percentage,
      BigDecimal minAttendancePercent) {}

  public record LowAttendanceReport(
      Long batchId, BigDecimal thresholdPercent, List<LowAttendanceRow> students) {}

  /** Convenience wrapper for RP-EX-01 UI (reuses exam result shape). */
  public record ExamResultReportRow(
      Long examId,
      String examName,
      String examStatus,
      LocalDate examDate,
      Long studentId,
      String studentCode,
      String studentName,
      String resultStatus,
      BigDecimal totalMarks,
      String passFail,
      String grade) {}

  /** RP-ST-04 — withdrawals / dropouts */
  public record WithdrawalReportRow(
      Long enrollmentId,
      Long studentId,
      String studentCode,
      String studentName,
      Long batchId,
      String batchCode,
      Long courseId,
      String courseCode,
      String enrollmentStatus,
      Instant withdrawnAt,
      Instant activatedAt) {}

  /** RP-ST-05 — batch-wise roster */
  public record BatchRosterRow(
      Long enrollmentId,
      Long studentId,
      String studentCode,
      String studentName,
      String phone,
      String email,
      Long batchId,
      String batchCode,
      String batchName,
      Long courseId,
      String courseCode,
      String enrollmentStatus,
      Instant activatedAt) {}
}
