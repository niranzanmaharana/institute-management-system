package com.ims.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecretKey key;
  private final long accessTokenMinutes;

  public JwtService(
      @Value("${ims.security.jwt-secret}") String secret,
      @Value("${ims.security.access-token-minutes:30}") long accessTokenMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenMinutes = accessTokenMinutes;
  }

  public String createAccessToken(
      Long userId,
      String username,
      Long instituteId,
      Collection<String> roles,
      Collection<String> permissions) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(accessTokenMinutes * 60);
    var builder =
        Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("roles", List.copyOf(roles))
            .claim("permissions", List.copyOf(permissions))
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp));
    if (instituteId != null) {
      builder.claim("institute_id", instituteId);
    }
    return builder.signWith(key).compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
