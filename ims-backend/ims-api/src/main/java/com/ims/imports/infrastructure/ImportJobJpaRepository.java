package com.ims.imports.infrastructure;

import com.ims.imports.domain.ImportJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobJpaRepository extends JpaRepository<ImportJob, Long> {

  Optional<ImportJob> findByIdAndInstituteId(Long id, Long instituteId);
}
