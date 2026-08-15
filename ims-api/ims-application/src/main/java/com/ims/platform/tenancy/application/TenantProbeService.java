package com.ims.platform.tenancy.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.platform.tenancy.domain.TenantProbe;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import com.ims.platform.tenancy.infrastructure.TenantProbeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantProbeService {

  private final TenantProbeJpaRepository repository;
  private final TenantFilterEnabler tenantFilterEnabler;

  public TenantProbeService(
      TenantProbeJpaRepository repository, TenantFilterEnabler tenantFilterEnabler) {
    this.repository = repository;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  @Transactional
  public TenantProbe create(String label) {
    long instituteId = TenantContext.requireInstituteId();
    return repository.save(new TenantProbe(instituteId, label));
  }

  @Transactional(readOnly = true)
  public Optional<TenantProbe> findByIdForCurrentTenant(Long id) {
    tenantFilterEnabler.enableForCurrentTenant();
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<TenantProbe> listForCurrentTenant() {
    long instituteId = TenantContext.requireInstituteId();
    return repository.findAllByInstituteId(instituteId);
  }
}
