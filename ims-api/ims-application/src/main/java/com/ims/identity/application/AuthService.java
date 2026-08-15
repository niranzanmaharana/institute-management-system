package com.ims.identity.application;

import com.ims.identity.domain.LoginAudit;
import com.ims.identity.domain.Permission;
import com.ims.identity.domain.RefreshToken;
import com.ims.identity.domain.Role;
import com.ims.identity.domain.UserAccount;
import com.ims.identity.infrastructure.LoginAuditRepository;
import com.ims.identity.infrastructure.RefreshTokenRepository;
import com.ims.identity.infrastructure.UserAccountRepository;
import com.ims.identity.security.JwtService;
import com.ims.platform.institute.infrastructure.InstituteJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserAccountRepository userAccountRepository;
  private final InstituteJpaRepository instituteJpaRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final LoginAuditRepository loginAuditRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final long refreshTokenDays;

  public AuthService(
      UserAccountRepository userAccountRepository,
      InstituteJpaRepository instituteJpaRepository,
      RefreshTokenRepository refreshTokenRepository,
      LoginAuditRepository loginAuditRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${ims.security.refresh-token-days:14}") long refreshTokenDays) {
    this.userAccountRepository = userAccountRepository;
    this.instituteJpaRepository = instituteJpaRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.loginAuditRepository = loginAuditRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenDays = refreshTokenDays;
  }

  public record AuthTokens(String accessToken, String refreshToken, UserView user) {}

  public record UserView(
      Long id, String username, String email, Long instituteId, Set<String> roles, Set<String> permissions) {}

  @Transactional
  public AuthTokens login(
      String username, String password, String instituteCode, String ip, String userAgent) {
    UserAccount user = resolveUser(username, instituteCode);
    if (user == null || !user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
      loginAuditRepository.save(new LoginAudit(null, username, false, ip, userAgent));
      log.warn(
          "Login failed username={} instituteCode={} ip={}",
          username,
          instituteCode == null || instituteCode.isBlank() ? "-" : instituteCode,
          ip == null ? "-" : ip);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    user.markLogin();
    userAccountRepository.save(user);
    loginAuditRepository.save(new LoginAudit(user.getId(), username, true, ip, userAgent));
    log.info(
        "Login success userId={} username={} instituteId={}",
        user.getId(),
        user.getUsername(),
        user.getInstituteId() == null ? "-" : user.getInstituteId());

    return issueTokens(user);
  }

  @Transactional
  public AuthTokens refresh(String refreshTokenRaw) {
    String hash = sha256(refreshTokenRaw);
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
    if (!stored.isUsable()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
    }
    UserAccount user =
        userAccountRepository
            .findById(stored.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    if (!user.isActive()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User inactive");
    }
    stored.revoke();
    refreshTokenRepository.save(stored);
    return issueTokens(user);
  }

  @Transactional
  public void logout(String refreshTokenRaw) {
    if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
      return;
    }
    refreshTokenRepository
        .findByTokenHash(sha256(refreshTokenRaw))
        .ifPresent(
            token -> {
              token.revoke();
              refreshTokenRepository.save(token);
            });
  }

  @Transactional(readOnly = true)
  public UserView me(Long userId) {
    UserAccount user =
        userAccountRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return toView(user);
  }

  private AuthTokens issueTokens(UserAccount user) {
    Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    Set<String> permissions =
        user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());

    String access =
        jwtService.createAccessToken(
            user.getId(), user.getUsername(), user.getInstituteId(), roles, permissions);
    String refreshRaw = UUID.randomUUID() + "." + UUID.randomUUID();
    Instant expiresAt = Instant.now().plusSeconds(refreshTokenDays * 24 * 3600);
    refreshTokenRepository.save(new RefreshToken(user.getId(), sha256(refreshRaw), expiresAt));
    return new AuthTokens(access, refreshRaw, toView(user));
  }

  private UserAccount resolveUser(String username, String instituteCode) {
    if (instituteCode == null || instituteCode.isBlank()) {
      return userAccountRepository
          .findByUsernameAndInstituteIdIsNullAndDeletedAtIsNull(username)
          .orElse(null);
    }
    Long instituteId =
        instituteJpaRepository
            .findByCode(instituteCode.trim().toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"))
            .getId();
    return userAccountRepository
        .findByUsernameAndInstituteIdAndDeletedAtIsNull(username, instituteId)
        .orElse(null);
  }

  private UserView toView(UserAccount user) {
    Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
    Set<String> permissions =
        user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());
    return new UserView(
        user.getId(), user.getUsername(), user.getEmail(), user.getInstituteId(), roles, permissions);
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
