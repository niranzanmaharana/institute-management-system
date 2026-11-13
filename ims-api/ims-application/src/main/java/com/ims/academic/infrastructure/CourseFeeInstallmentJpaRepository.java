package com.ims.academic.infrastructure;

import com.ims.academic.domain.CourseFeeInstallment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseFeeInstallmentJpaRepository extends JpaRepository<CourseFeeInstallment, Long> {
  List<CourseFeeInstallment> findByFeePlanIdAndInstituteIdOrderBySeqAsc(
      Long feePlanId, Long instituteId);

  /** Bulk delete + flush/clear so replace can re-insert the same (fee_plan_id, seq). */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "delete from CourseFeeInstallment i where i.feePlanId = :feePlanId and i.instituteId = :instituteId")
  void deleteByFeePlanIdAndInstituteId(
      @Param("feePlanId") Long feePlanId, @Param("instituteId") Long instituteId);
}
