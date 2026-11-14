package com.ims.operations.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

  private AttendanceDtos() {}

  public record AttendanceSessionResponse(
      Long id,
      Long batchId,
      LocalDate sessionDate,
      Long subjectId,
      String status,
      Long markedBy,
      Instant lockedAt,
      Instant unlockedAt,
      Instant createdAt,
      List<AttendanceRecordResponse> records) {}

  public record AttendanceRecordResponse(
      Long id,
      Long studentId,
      String studentCode,
      String studentName,
      String status,
      String note,
      boolean eligible) {}

  public record StudentAttendanceSummary(
      Long studentId,
      Long batchId,
      long presentEffective,
      long absent,
      long excused,
      long sessionsCounted,
      BigDecimal percentage,
      BigDecimal minAttendancePercent,
      boolean belowThreshold) {}

  public record BatchAttendanceSummary(
      Long batchId,
      long sessionCount,
      long draftCount,
      long submittedCount,
      long lockedCount,
      List<StudentAttendanceSummary> students) {}
}
