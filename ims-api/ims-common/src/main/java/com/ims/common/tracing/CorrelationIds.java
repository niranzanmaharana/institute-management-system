package com.ims.common.tracing;

import org.slf4j.MDC;

/** Correlation fields shared by gateway and application logs / error payloads. */
public final class CorrelationIds {

  public static final String TRACE_ID = "traceId";
  public static final String SPAN_ID = "spanId";
  public static final String REQUEST_ID = "requestId";
  public static final String USER_ID = "userId";
  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  private CorrelationIds() {}

  public static String traceId() {
    return blankToDash(MDC.get(TRACE_ID));
  }

  /** For API error payloads — omit placeholder dash. */
  public static String traceIdOrNull() {
    String value = MDC.get(TRACE_ID);
    return value == null || value.isBlank() || "-".equals(value) ? null : value;
  }

  public static String spanId() {
    return blankToDash(MDC.get(SPAN_ID));
  }

  public static String requestId() {
    return blankToDash(MDC.get(REQUEST_ID));
  }

  public static String userId() {
    return blankToDash(MDC.get(USER_ID));
  }

  public static void putRequestId(String requestId) {
    if (requestId != null && !requestId.isBlank()) {
      MDC.put(REQUEST_ID, requestId.trim());
    }
  }

  public static void putUserId(String userId) {
    if (userId != null && !userId.isBlank()) {
      MDC.put(USER_ID, userId);
    }
  }

  public static void putTrace(String traceId, String spanId) {
    if (traceId != null && !traceId.isBlank()) {
      MDC.put(TRACE_ID, traceId);
    }
    if (spanId != null && !spanId.isBlank()) {
      MDC.put(SPAN_ID, spanId);
    }
  }

  public static void clearRequestScoped() {
    MDC.remove(REQUEST_ID);
    MDC.remove(USER_ID);
  }

  public static void clearAllCorrelation() {
    MDC.remove(TRACE_ID);
    MDC.remove(SPAN_ID);
    MDC.remove(REQUEST_ID);
    MDC.remove(USER_ID);
  }

  private static String blankToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
