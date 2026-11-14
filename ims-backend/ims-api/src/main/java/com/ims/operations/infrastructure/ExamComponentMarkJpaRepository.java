package com.ims.operations.infrastructure;

import com.ims.operations.domain.ExamComponentMark;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamComponentMarkJpaRepository extends JpaRepository<ExamComponentMark, Long> {

  List<ExamComponentMark> findByInstituteIdAndExamResultIdIn(
      Long instituteId, Collection<Long> examResultIds);

  Optional<ExamComponentMark> findByInstituteIdAndExamResultIdAndExamComponentId(
      Long instituteId, Long examResultId, Long examComponentId);
}
