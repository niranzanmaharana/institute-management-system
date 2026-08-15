package com.ims.finance.infrastructure;

import com.ims.finance.domain.Invoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long> {

  List<Invoice> findByFeeAccountIdAndInstituteIdOrderByDueDateAscIdAsc(
      Long feeAccountId, Long instituteId);

  List<Invoice> findByFeeAccountIdAndInstituteIdAndStatusInOrderByDueDateAscIdAsc(
      Long feeAccountId, Long instituteId, List<String> statuses);
}
