package com.ims.academic.infrastructure;

import com.ims.academic.domain.FeeCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeCategoryJpaRepository extends JpaRepository<FeeCategory, Long> {
  boolean existsByInstituteIdAndCodeIgnoreCase(Long instituteId, String code);

  List<FeeCategory> findByInstituteIdOrderByCodeAsc(Long instituteId);

  Optional<FeeCategory> findByIdAndInstituteId(Long id, Long instituteId);
}
