package com.ims.operations.infrastructure;

import com.ims.operations.domain.ExamComponent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamComponentJpaRepository extends JpaRepository<ExamComponent, Long> {

  List<ExamComponent> findByInstituteIdAndExamIdOrderBySeqAscIdAsc(Long instituteId, Long examId);

  Optional<ExamComponent> findByIdAndInstituteId(Long id, Long instituteId);

  boolean existsByExamIdAndCodeIgnoreCase(Long examId, String code);

  long countByInstituteIdAndExamId(Long instituteId, Long examId);
}
