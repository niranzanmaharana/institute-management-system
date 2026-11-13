package com.ims.finance.infrastructure;

import com.ims.finance.domain.Payment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByIdAndInstituteId(Long id, Long instituteId);

  Optional<Payment> findByInstituteIdAndIdempotencyKey(Long instituteId, String idempotencyKey);

  List<Payment> findByFeeAccountIdAndInstituteIdAndStatus(
      Long feeAccountId, Long instituteId, String status);

  List<Payment> findByFeeAccountIdAndInstituteIdOrderByPaidAtDesc(
      Long feeAccountId, Long instituteId);

  List<Payment> findByInstituteIdAndStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
      Long instituteId, String status, Instant fromInclusive, Instant toExclusive);
}
