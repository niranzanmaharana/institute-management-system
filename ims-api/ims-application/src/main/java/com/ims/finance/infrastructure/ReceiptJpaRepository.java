package com.ims.finance.infrastructure;

import com.ims.finance.domain.Receipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptJpaRepository extends JpaRepository<Receipt, Long> {

  Optional<Receipt> findByInstituteIdAndPaymentId(Long instituteId, Long paymentId);
}
