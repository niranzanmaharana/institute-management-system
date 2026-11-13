package com.ims.academic.infrastructure;

import com.ims.academic.domain.Subject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectJpaRepository extends JpaRepository<Subject, Long> {

  List<Subject> findByInstituteIdAndCourseIdOrderByCodeAsc(Long instituteId, Long courseId);

  boolean existsByInstituteIdAndCourseIdAndCodeIgnoreCase(Long instituteId, Long courseId, String code);

  java.util.Optional<Subject> findByIdAndInstituteId(Long id, Long instituteId);

  long countByInstituteIdAndCourseId(Long instituteId, Long courseId);
}
