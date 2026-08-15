package com.ims.finance.infrastructure;

import com.ims.finance.domain.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationJpaRepository extends JpaRepository<PaymentAllocation, Long> {}
