package com.ims.config;

import com.ims.common.error.ApiError;
import com.ims.common.tracing.CorrelationIds;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<ApiError.FieldErrorDetail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> new ApiError.FieldErrorDetail(err.getField(), err.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(
            ApiError.of(
                400, "VALIDATION_ERROR", "Request validation failed", traceId(), details));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex) {
    int status = ex.getStatusCode().value();
    return ResponseEntity.status(status)
        .body(
            ApiError.of(
                status,
                HttpStatus.valueOf(status).name(),
                ex.getReason() == null ? ex.getMessage() : ex.getReason(),
                traceId(),
                null));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiError.of(
                403,
                "FORBIDDEN",
                "You do not have permission for this action",
                traceId(),
                List.of()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiError.of(401, "UNAUTHORIZED", "Authentication required", traceId(), null));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of(400, "ILLEGAL_STATE", ex.getMessage(), traceId(), null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
    org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
        .error("Unhandled error on {}", request.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiError.of(
                500, "INTERNAL_ERROR", "Unexpected server error", traceId(), null));
  }

  private String traceId() {
    return CorrelationIds.traceIdOrNull();
  }
}
