package com.ims.operations.infrastructure;

import com.ims.operations.domain.Exam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamJpaRepository extends JpaRepository<Exam, Long> {

  Optional<Exam> findByIdAndInstituteId(Long id, Long instituteId);

  List<Exam> findByInstituteIdAndBatchIdOrderByExamDateDescIdDesc(Long instituteId, Long batchId);

  List<Exam> findByInstituteIdOrderByExamDateDescIdDesc(Long instituteId);

  List<Exam> findByInstituteIdAndBatchIdAndStatusInOrderByExamDateDescIdDesc(
      Long instituteId, Long batchId, java.util.Collection<String> statuses);
}
