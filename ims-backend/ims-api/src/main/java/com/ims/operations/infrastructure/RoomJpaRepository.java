package com.ims.operations.infrastructure;

import com.ims.operations.domain.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<Room, Long> {

  Optional<Room> findByIdAndInstituteId(Long id, Long instituteId);

  List<Room> findByInstituteIdOrderByCodeAsc(Long instituteId);

  List<Room> findByInstituteIdAndStatusOrderByCodeAsc(Long instituteId, String status);

  boolean existsByInstituteIdAndCodeIgnoreCase(Long instituteId, String code);
}
