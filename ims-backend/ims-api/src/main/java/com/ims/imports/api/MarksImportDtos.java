package com.ims.imports.api;

import java.time.Instant;

public final class MarksImportDtos {

  private MarksImportDtos() {}

  public record MarksImportJobResponse(
      Long id,
      Long examId,
      String status,
      String fileName,
      int totalRows,
      int successCount,
      int failedCount,
      String errorReportCsv,
      String message,
      boolean async,
      Instant createdAt,
      Instant startedAt,
      Instant completedAt) {}
}
