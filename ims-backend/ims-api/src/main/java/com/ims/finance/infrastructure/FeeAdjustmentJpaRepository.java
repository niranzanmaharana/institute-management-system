package com.ims.finance.infrastructure;

import com.ims.finance.domain.FeeAdjustment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeeAdjustmentJpaRepository extends JpaRepository<FeeAdjustment, Long> {

  List<FeeAdjustment> findByFeeAccountIdAndInstituteIdOrderByCreatedAtDesc(
      Long feeAccountId, Long instituteId);

  @Query(
      """
      SELECT a FROM FeeAdjustment a
      WHERE a.instituteId = :instituteId
        AND a.type IN :types
        AND a.createdAt >= :fromInclusive
        AND a.createdAt < :toExclusive
      ORDER BY a.createdAt DESC
      """)
  List<FeeAdjustment> findCreditsInRange(
      @Param("instituteId") Long instituteId,
      @Param("types") Collection<String> types,
      @Param("fromInclusive") Instant fromInclusive,
      @Param("toExclusive") Instant toExclusive);
}
