package com.ims.people.student.infrastructure;

import com.ims.people.student.domain.Guardian;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianJpaRepository extends JpaRepository<Guardian, Long> {

  Optional<Guardian> findByInstituteIdAndPhoneAndDeletedAtIsNull(Long instituteId, String phone);

  Optional<Guardian> findByInstituteIdAndEmailIgnoreCaseAndDeletedAtIsNull(
      Long instituteId, String email);

  Optional<Guardian> findByIdAndInstituteIdAndDeletedAtIsNull(Long id, Long instituteId);
}
