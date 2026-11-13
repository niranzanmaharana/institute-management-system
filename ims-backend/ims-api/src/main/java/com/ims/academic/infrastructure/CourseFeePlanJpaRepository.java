package com.ims.academic.infrastructure;

import com.ims.academic.domain.CourseFeePlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseFeePlanJpaRepository extends JpaRepository<CourseFeePlan, Long> {
  boolean existsByInstituteIdAndCodeIgnoreCase(Long instituteId, String code);

  List<CourseFeePlan> findByInstituteIdAndCourseIdOrderByCodeAsc(Long instituteId, Long courseId);

  Optional<CourseFeePlan> findByIdAndInstituteId(Long id, Long instituteId);
}
