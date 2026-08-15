package com.ims.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    Instant timestamp,
    String traceId,
    int status,
    String error,
    String message,
    List<FieldErrorDetail> details) {

  public record FieldErrorDetail(String field, String message) {}

  public static ApiError of(
      int status, String error, String message, String traceId, List<FieldErrorDetail> details) {
    return new ApiError(Instant.now(), traceId, status, error, message, details);
  }
}
