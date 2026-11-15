package com.ims.academic.infrastructure;

import com.ims.academic.domain.Enrollment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {

  Optional<Enrollment> findByIdAndInstituteId(Long id, Long instituteId);

  List<Enrollment> findByInstituteIdOrderByCreatedAtDesc(Long instituteId);

  List<Enrollment> findByInstituteIdAndStatusOrderByCreatedAtDesc(Long instituteId, String status);

  List<Enrollment> findByInstituteIdAndBatchIdOrderByCreatedAtDesc(Long instituteId, Long batchId);

  List<Enrollment> findByInstituteIdAndCourseIdOrderByCreatedAtDesc(Long instituteId, Long courseId);

  boolean existsByInstituteIdAndStudentIdAndCourseIdAndStatusIn(
      Long instituteId, Long studentId, Long courseId, Collection<String> statuses);

  long countByInstituteIdAndBatchIdAndStatus(Long instituteId, Long batchId, String status);

  long countByInstituteIdAndCourseIdAndStatus(Long instituteId, Long courseId, String status);

  List<Enrollment> findByInstituteIdAndBatchIdAndStatusOrderByActivatedAtAscIdAsc(
      Long instituteId, Long batchId, String status);

  List<Enrollment>
      findByInstituteIdAndStatusAndWithdrawnAtGreaterThanEqualAndWithdrawnAtLessThanOrderByWithdrawnAtDesc(
          Long instituteId, String status, Instant withdrawnFromInclusive, Instant withdrawnToExclusive);
}
