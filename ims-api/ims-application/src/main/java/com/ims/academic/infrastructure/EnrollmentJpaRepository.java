package com.ims.academic.infrastructure;

import com.ims.academic.domain.Enrollment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {

  Optional<Enrollment> findByIdAndInstituteId(Long id, Long instituteId);

  List<Enrollment> findByInstituteIdOrderByCreatedAtDesc(Long instituteId);

  boolean existsByInstituteIdAndStudentIdAndCourseIdAndStatusIn(
      Long instituteId, Long studentId, Long courseId, Collection<String> statuses);

  long countByInstituteIdAndBatchIdAndStatus(Long instituteId, Long batchId, String status);
}
