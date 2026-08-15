package com.ims.finance.infrastructure;

import com.ims.finance.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByInstituteIdAndIdempotencyKey(Long instituteId, String idempotencyKey);
}
