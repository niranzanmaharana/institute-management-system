package com.ims.academic.infrastructure;

import com.ims.academic.domain.AcademicYear;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearJpaRepository extends JpaRepository<AcademicYear, Long> {
  boolean existsByInstituteIdAndCodeIgnoreCase(Long instituteId, String code);

  List<AcademicYear> findByInstituteIdOrderByStartDateDesc(Long instituteId);

  Optional<AcademicYear> findByIdAndInstituteId(Long id, Long instituteId);
}
