package com.ims.gateway.filter;

import com.ims.common.tracing.CorrelationIds;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Ensures {@code X-Request-Id} and logs every proxied request with the same correlation fields
 * used by {@code ims-application} (traceId, spanId, requestId).
 */
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

  private final Tracer tracer;
  private final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();

  public RequestLoggingGlobalFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst(CorrelationIds.REQUEST_ID_HEADER);
    String requestId =
        (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();

    ServerHttpRequest request =
        exchange.getRequest().mutate().header(CorrelationIds.REQUEST_ID_HEADER, requestId).build();
    exchange.getResponse().getHeaders().set(CorrelationIds.REQUEST_ID_HEADER, requestId);

    ServerWebExchange mutated = exchange.mutate().request(request).build();
    long started = System.nanoTime();

    return chain
        .filter(mutated)
        .doOnEach(
            signal -> {
              if (!signal.isOnComplete() && !signal.isOnError()) {
                return;
              }
              logAccess(mutated, requestId, started, signal.getContextView(), signal.isOnError());
            });
  }

  private void logAccess(
      ServerWebExchange exchange,
      String requestId,
      long startedNanos,
      ContextView contextView,
      boolean error) {
    if (shouldSkip(exchange.getRequest().getURI().getPath())) {
      return;
    }
    try (ContextSnapshot.Scope ignored = snapshotFactory.setThreadLocalsFrom(contextView)) {
      Span span = tracer.currentSpan();
      if (span != null) {
        CorrelationIds.putTrace(span.context().traceId(), span.context().spanId());
      }
      CorrelationIds.putRequestId(requestId);
      Integer status =
          exchange.getResponse().getStatusCode() != null
              ? exchange.getResponse().getStatusCode().value()
              : null;
      long durationMs = (System.nanoTime() - startedNanos) / 1_000_000L;
      log.info(
          "HTTP {} {} status={} durationMs={} outcome={} requestId={} traceId={} spanId={}",
          exchange.getRequest().getMethod(),
          exchange.getRequest().getURI().getPath(),
          status == null ? "-" : status,
          durationMs,
          error ? "ERROR" : "SUCCESS",
          requestId,
          CorrelationIds.traceId(),
          CorrelationIds.spanId());
    } finally {
      CorrelationIds.clearAllCorrelation();
    }
  }

  private boolean shouldSkip(String path) {
    return path != null
        && (path.startsWith("/actuator/health") || path.startsWith("/actuator/info"));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 5;
  }
}
