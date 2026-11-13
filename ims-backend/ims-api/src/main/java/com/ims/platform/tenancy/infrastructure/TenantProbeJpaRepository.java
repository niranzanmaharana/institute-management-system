package com.ims.platform.tenancy.infrastructure;

import com.ims.platform.tenancy.domain.TenantProbe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantProbeJpaRepository extends JpaRepository<TenantProbe, Long> {
  Optional<TenantProbe> findByIdAndInstituteId(Long id, Long instituteId);

  List<TenantProbe> findAllByInstituteId(Long instituteId);
}
