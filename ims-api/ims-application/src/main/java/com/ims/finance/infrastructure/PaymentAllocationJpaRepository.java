package com.ims.finance.infrastructure;

import com.ims.finance.domain.PaymentAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationJpaRepository extends JpaRepository<PaymentAllocation, Long> {

  List<PaymentAllocation> findByPaymentIdAndInstituteId(Long paymentId, Long instituteId);

  List<PaymentAllocation> findByInstituteIdAndPaymentIdIn(Long instituteId, List<Long> paymentIds);
}
