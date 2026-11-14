package com.ims.operations.infrastructure;

import com.ims.operations.domain.AttendanceRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordJpaRepository extends JpaRepository<AttendanceRecord, Long> {

  List<AttendanceRecord> findByInstituteIdAndSessionIdOrderByStudentIdAsc(
      Long instituteId, Long sessionId);

  Optional<AttendanceRecord> findByInstituteIdAndSessionIdAndStudentId(
      Long instituteId, Long sessionId, Long studentId);

  List<AttendanceRecord> findByInstituteIdAndStudentIdAndSessionIdIn(
      Long instituteId, Long studentId, Collection<Long> sessionIds);

  List<AttendanceRecord> findByInstituteIdAndSessionIdIn(Long instituteId, Collection<Long> sessionIds);

  long countByInstituteIdAndSessionId(Long instituteId, Long sessionId);
}
