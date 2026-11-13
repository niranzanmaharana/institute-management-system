package com.ims.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * In-memory sliding-window rate limit for login (local/dev friendly; replace with Redis in prod).
 */
@Component
public class LoginRateLimitGlobalFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(LoginRateLimitGlobalFilter.class);

  private final int maxAttempts;
  private final Duration window;
  private final Map<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();

  public LoginRateLimitGlobalFilter(
      @Value("${ims.gateway.login-rate-limit.max-attempts:20}") int maxAttempts,
      @Value("${ims.gateway.login-rate-limit.window-seconds:60}") long windowSeconds) {
    this.maxAttempts = maxAttempts;
    this.window = Duration.ofSeconds(windowSeconds);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    if (request.getMethod() != HttpMethod.POST
        || !"/api/v1/auth/login".equals(request.getURI().getPath())) {
      return chain.filter(exchange);
    }

    String key = clientKey(request);
    long now = System.currentTimeMillis();
    long cutoff = now - window.toMillis();
    Deque<Long> queue = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (queue) {
      while (!queue.isEmpty() && queue.peekFirst() < cutoff) {
        queue.pollFirst();
      }
      if (queue.size() >= maxAttempts) {
        log.warn("Login rate-limited clientKey={}", key);
        return tooManyRequests(exchange);
      }
      queue.addLast(now);
    }
    return chain.filter(exchange);
  }

  private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body =
        """
        {"status":429,"code":"RATE_LIMITED","message":"Too many login attempts. Try again shortly."}
        """
            .getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  private static String clientKey(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
      return request.getRemoteAddress().getAddress().getHostAddress();
    }
    return "unknown";
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
