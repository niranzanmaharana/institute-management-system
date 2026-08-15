package com.ims.academic.infrastructure;

import com.ims.academic.domain.CourseFeeInstallment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseFeeInstallmentJpaRepository extends JpaRepository<CourseFeeInstallment, Long> {
  List<CourseFeeInstallment> findByFeePlanIdAndInstituteIdOrderBySeqAsc(Long feePlanId, Long instituteId);

  void deleteByFeePlanIdAndInstituteId(Long feePlanId, Long instituteId);
}
