package com.ims.platform.codes.infrastructure;

import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.codes.domain.CodeSequence;
import com.ims.platform.codes.domain.CodeSequenceId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSequenceJpaRepository extends JpaRepository<CodeSequence, CodeSequenceId> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT s FROM CodeSequence s
      WHERE s.instituteId = :instituteId AND s.entityType = :entityType
      """)
  Optional<CodeSequence> findForUpdate(
      @Param("instituteId") Long instituteId, @Param("entityType") CodeEntityType entityType);
}
