package com.ims.operations.infrastructure;

import com.ims.operations.domain.ExamResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultJpaRepository extends JpaRepository<ExamResult, Long> {

  List<ExamResult> findByInstituteIdAndExamIdOrderByStudentIdAsc(Long instituteId, Long examId);

  Optional<ExamResult> findByInstituteIdAndExamIdAndStudentId(
      Long instituteId, Long examId, Long studentId);

  List<ExamResult> findByInstituteIdAndStudentIdAndExamIdIn(
      Long instituteId, Long studentId, Collection<Long> examIds);

  long countByInstituteIdAndExamId(Long instituteId, Long examId);
}
