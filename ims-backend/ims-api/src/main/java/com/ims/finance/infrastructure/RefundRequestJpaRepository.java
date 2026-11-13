package com.ims.finance.infrastructure;

import com.ims.finance.domain.RefundRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestJpaRepository extends JpaRepository<RefundRequest, Long> {

  Optional<RefundRequest> findByIdAndInstituteId(Long id, Long instituteId);

  List<RefundRequest> findByInstituteIdOrderByCreatedAtDesc(Long instituteId);

  List<RefundRequest> findByInstituteIdAndFeeAccountIdOrderByCreatedAtDesc(
      Long instituteId, Long feeAccountId);

  List<RefundRequest> findByInstituteIdAndStatusOrderByCreatedAtDesc(Long instituteId, String status);

  Optional<RefundRequest> findByInstituteIdAndProcessIdempotencyKey(
      Long instituteId, String processIdempotencyKey);

  List<RefundRequest> findByFeeAccountIdAndInstituteIdAndStatusIn(
      Long feeAccountId, Long instituteId, List<String> statuses);
}
