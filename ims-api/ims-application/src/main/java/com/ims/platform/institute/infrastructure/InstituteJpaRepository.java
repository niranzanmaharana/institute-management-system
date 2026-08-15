package com.ims.platform.institute.infrastructure;

import com.ims.platform.institute.domain.Institute;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituteJpaRepository extends JpaRepository<Institute, Long> {
  Optional<Institute> findByCode(String code);

  long countByStatus(String status);
}
