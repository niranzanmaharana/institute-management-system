package com.ims.operations.infrastructure;

import com.ims.operations.domain.TimetableSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableSlotJpaRepository extends JpaRepository<TimetableSlot, Long> {

  Optional<TimetableSlot> findByIdAndInstituteId(Long id, Long instituteId);

  List<TimetableSlot> findByInstituteIdAndBatchIdAndStatusOrderByDayOfWeekAscStartTimeAsc(
      Long instituteId, Long batchId, String status);

  List<TimetableSlot> findByInstituteIdAndRoomIdAndDayOfWeekAndStatus(
      Long instituteId, Long roomId, int dayOfWeek, String status);

  List<TimetableSlot> findByInstituteIdAndFacultyIdAndDayOfWeekAndStatus(
      Long instituteId, Long facultyId, int dayOfWeek, String status);

  List<TimetableSlot> findByInstituteIdAndBatchIdAndDayOfWeekAndStatusOrderByStartTimeAsc(
      Long instituteId, Long batchId, int dayOfWeek, String status);
}
