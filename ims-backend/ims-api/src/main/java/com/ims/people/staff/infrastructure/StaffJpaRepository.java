package com.ims.people.staff.infrastructure;

import com.ims.people.staff.domain.Staff;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffJpaRepository extends JpaRepository<Staff, Long> {

  boolean existsByInstituteIdAndStaffCodeIgnoreCase(Long instituteId, String staffCode);

  Optional<Staff> findByIdAndInstituteIdAndDeletedAtIsNull(Long id, Long instituteId);

  @Query(
      """
      SELECT s FROM Staff s
      WHERE s.instituteId = :instituteId
        AND (
          (:status IS NULL AND s.deletedAt IS NULL)
          OR (:status = 'ACTIVE' AND s.status = 'ACTIVE' AND s.deletedAt IS NULL)
          OR (:status = 'INACTIVE' AND s.status = 'INACTIVE')
        )
        AND (
          :q IS NULL OR :q = ''
          OR LOWER(s.staffCode) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.designation, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  Page<Staff> search(
      @Param("instituteId") Long instituteId,
      @Param("q") String q,
      @Param("status") String status,
      Pageable pageable);
}
