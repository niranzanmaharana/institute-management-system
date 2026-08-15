package com.ims.platform.audit.infrastructure;

import com.ims.platform.audit.domain.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findByInstituteIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
      Long instituteId, String entityType, Long entityId);

  long countByInstituteIdAndAction(Long instituteId, String action);
}
