package com.ims.config;

import com.ims.common.tracing.CorrelationIds;
import com.ims.identity.security.ImsUserPrincipal;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sets {@code X-Request-Id} / MDC and logs each HTTP call with traceId, spanId, requestId.
 *
 * <p>Ordered after Spring's {@code ServerHttpObservationFilter} so Micrometer span MDC is active
 * while we log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  private final Tracer tracer;

  public RequestLoggingFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(CorrelationIds.REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    CorrelationIds.putRequestId(requestId);
    response.setHeader(CorrelationIds.REQUEST_ID_HEADER, requestId);

    long started = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      syncTraceFromTracer();
      putAuthenticatedUser();
      long durationMs = (System.nanoTime() - started) / 1_000_000L;
      if (!shouldSkip(request.getRequestURI())) {
        log.info(
            "HTTP {} {} status={} durationMs={} requestId={} traceId={} spanId={} userId={}",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            durationMs,
            CorrelationIds.requestId(),
            CorrelationIds.traceId(),
            CorrelationIds.spanId(),
            CorrelationIds.userId());
      }
      CorrelationIds.clearRequestScoped();
    }
  }

  private void syncTraceFromTracer() {
    Span span = tracer.currentSpan();
    if (span != null) {
      CorrelationIds.putTrace(span.context().traceId(), span.context().spanId());
    }
  }

  private void putAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof ImsUserPrincipal principal) {
      CorrelationIds.putUserId(String.valueOf(principal.getUserId()));
    }
  }

  private boolean shouldSkip(String uri) {
    return uri != null && (uri.startsWith("/actuator/health") || uri.startsWith("/actuator/info"));
  }
}
