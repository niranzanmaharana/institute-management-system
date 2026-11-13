package com.ims.finance.infrastructure;

import com.ims.finance.domain.FeeAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeAdjustmentJpaRepository extends JpaRepository<FeeAdjustment, Long> {

  List<FeeAdjustment> findByFeeAccountIdAndInstituteIdOrderByCreatedAtDesc(
      Long feeAccountId, Long instituteId);
}
