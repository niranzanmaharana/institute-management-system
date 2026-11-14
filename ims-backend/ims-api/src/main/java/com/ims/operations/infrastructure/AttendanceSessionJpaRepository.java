package com.ims.operations.infrastructure;

import com.ims.operations.domain.AttendanceSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSessionJpaRepository extends JpaRepository<AttendanceSession, Long> {

  Optional<AttendanceSession> findByIdAndInstituteId(Long id, Long instituteId);

  List<AttendanceSession> findByInstituteIdAndBatchIdOrderBySessionDateDescIdDesc(
      Long instituteId, Long batchId);

  List<AttendanceSession>
      findByInstituteIdAndBatchIdAndSessionDateBetweenOrderBySessionDateDescIdDesc(
          Long instituteId, Long batchId, LocalDate from, LocalDate to);

  List<AttendanceSession> findByInstituteIdOrderBySessionDateDescIdDesc(Long instituteId);

  boolean existsByInstituteIdAndBatchIdAndSessionDateAndSubjectId(
      Long instituteId, Long batchId, LocalDate sessionDate, Long subjectId);

  boolean existsByInstituteIdAndBatchIdAndSessionDateAndSubjectIdIsNull(
      Long instituteId, Long batchId, LocalDate sessionDate);
}
