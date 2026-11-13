package com.ims.platform.tenancy.infrastructure;

import com.ims.common.tenancy.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class TenantFilterEnabler {

  private final EntityManager entityManager;

  public TenantFilterEnabler(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void enableForCurrentTenant() {
    Long instituteId = TenantContext.requireInstituteId();
    Session session = entityManager.unwrap(Session.class);
    session
        .enableFilter("instituteFilter")
        .setParameter("instituteId", instituteId);
  }
}
