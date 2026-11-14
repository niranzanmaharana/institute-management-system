package com.ims.operations.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ExamDtos {

  private ExamDtos() {}

  public record ExamComponentResponse(
      Long id,
      Long subjectId,
      String code,
      String name,
      BigDecimal maxMarks,
      BigDecimal passMarks,
      BigDecimal weightage,
      int seq) {}

  public record ExamResponse(
      Long id,
      Long batchId,
      String name,
      LocalDate examDate,
      String status,
      Long originalExamId,
      Instant publishedAt,
      Instant lockedAt,
      List<ExamComponentResponse> components) {}

  public record ComponentMarkResponse(Long componentId, String componentCode, BigDecimal marks) {}

  public record ExamResultResponse(
      Long id,
      Long examId,
      Long studentId,
      String studentCode,
      String studentName,
      String resultStatus,
      BigDecimal totalMarks,
      BigDecimal graceMarks,
      String passFail,
      String grade,
      String remarks,
      List<ComponentMarkResponse> componentMarks) {}

  public record BatchExamResultsSummary(
      Long batchId, Long examId, String examName, String status, List<ExamResultResponse> results) {}
}
