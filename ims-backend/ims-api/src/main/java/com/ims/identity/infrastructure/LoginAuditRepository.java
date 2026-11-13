package com.ims.identity.infrastructure;

import com.ims.identity.domain.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {}
