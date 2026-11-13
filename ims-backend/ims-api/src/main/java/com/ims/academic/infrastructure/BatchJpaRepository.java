package com.ims.academic.infrastructure;

import com.ims.academic.domain.Batch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchJpaRepository extends JpaRepository<Batch, Long> {
  boolean existsByInstituteIdAndCodeIgnoreCase(Long instituteId, String code);

  List<Batch> findByInstituteIdOrderByCodeAsc(Long instituteId);

  List<Batch> findByInstituteIdAndCourseIdOrderByCodeAsc(Long instituteId, Long courseId);

  Optional<Batch> findByIdAndInstituteId(Long id, Long instituteId);

  long countByInstituteIdAndCourseIdAndStatus(Long instituteId, Long courseId, String status);
}
