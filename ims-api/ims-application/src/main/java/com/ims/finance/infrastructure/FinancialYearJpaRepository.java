package com.ims.finance.infrastructure;

import com.ims.finance.domain.FinancialYear;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialYearJpaRepository extends JpaRepository<FinancialYear, Long> {

  Optional<FinancialYear> findFirstByInstituteIdAndActiveTrue(Long instituteId);
}
