package com.ims.finance.infrastructure;

import com.ims.finance.domain.StudentFeeAccount;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentFeeAccountJpaRepository extends JpaRepository<StudentFeeAccount, Long> {

  Optional<StudentFeeAccount> findByIdAndInstituteId(Long id, Long instituteId);

  Optional<StudentFeeAccount> findByInstituteIdAndEnrollmentId(Long instituteId, Long enrollmentId);

  List<StudentFeeAccount> findByInstituteIdAndStudentId(Long instituteId, Long studentId);

  List<StudentFeeAccount> findByInstituteIdAndStatusAndOutstandingAmountGreaterThan(
      Long instituteId, String status, BigDecimal amount);
}
