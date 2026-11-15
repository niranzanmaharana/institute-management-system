package com.ims.imports.api;

import java.util.List;

public final class ImportDtos {

  private ImportDtos() {}

  public record ImportRowError(int rowNumber, String field, String message, String raw) {}

  public record ImportResultResponse(
      String entityType,
      int totalRows,
      int inserted,
      int failed,
      List<Long> insertedIds,
      List<ImportRowError> errors,
      String errorReportCsv) {}
}
