package com.ims.finance.infrastructure;

import com.ims.finance.domain.FinancialYear;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialYearJpaRepository extends JpaRepository<FinancialYear, Long> {

  Optional<FinancialYear> findFirstByInstituteIdAndActiveTrue(Long instituteId);

  Optional<FinancialYear> findByIdAndInstituteId(Long id, Long instituteId);

  List<FinancialYear> findByInstituteIdOrderByStartDateDesc(Long instituteId);

  boolean existsByInstituteIdAndNameIgnoreCase(Long instituteId, String name);

  @Modifying
  @Query("update FinancialYear y set y.active = false, y.updatedAt = CURRENT_TIMESTAMP where y.instituteId = :instituteId and y.active = true")
  int deactivateAll(@Param("instituteId") Long instituteId);
}
