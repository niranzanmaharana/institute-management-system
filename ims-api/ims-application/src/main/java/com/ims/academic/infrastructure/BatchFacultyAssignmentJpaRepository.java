package com.ims.academic.infrastructure;

import com.ims.academic.domain.BatchFacultyAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchFacultyAssignmentJpaRepository
    extends JpaRepository<BatchFacultyAssignment, Long> {

  List<BatchFacultyAssignment> findByInstituteIdAndBatchIdOrderByCreatedAtDesc(
      Long instituteId, Long batchId);

  Optional<BatchFacultyAssignment> findByIdAndInstituteId(Long id, Long instituteId);

  boolean existsByInstituteIdAndBatchIdAndFacultyIdAndSubjectId(
      Long instituteId, Long batchId, Long facultyId, Long subjectId);

  boolean existsByInstituteIdAndBatchIdAndFacultyIdAndSubjectIdIsNull(
      Long instituteId, Long batchId, Long facultyId);
}
