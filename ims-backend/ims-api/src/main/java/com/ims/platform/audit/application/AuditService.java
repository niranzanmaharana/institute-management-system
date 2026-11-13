package com.ims.platform.audit.application;

import com.ims.common.tracing.CorrelationIds;
import com.ims.platform.audit.domain.AuditLog;
import com.ims.platform.audit.infrastructure.AuditLogJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ims.identity.security.ImsUserPrincipal;

@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditLogJpaRepository auditLogJpaRepository;
  private final ObjectMapper objectMapper;

  public AuditService(AuditLogJpaRepository auditLogJpaRepository, ObjectMapper objectMapper) {
    this.auditLogJpaRepository = auditLogJpaRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void record(
      Long instituteId,
      String module,
      String entityType,
      Long entityId,
      String action,
      Object before,
      Object after,
      String reason) {
    Long actorId = currentUserId();
    AuditLog row =
        AuditLog.of(
            instituteId,
            module,
            entityType,
            entityId,
            action,
            toJson(before),
            toJson(after),
            reason,
            actorId,
            CorrelationIds.requestId().equals("-") ? null : CorrelationIds.requestId());
    auditLogJpaRepository.save(row);
    log.info(
        "Audit recorded module={} entityType={} entityId={} action={} instituteId={}",
        module,
        entityType,
        entityId,
        action,
        instituteId);
  }

  private Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof ImsUserPrincipal principal) {
      return principal.getUserId();
    }
    return null;
  }

  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String s) {
      return s;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return String.valueOf(value);
    }
  }
}
