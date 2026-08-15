package com.ims.admissions.infrastructure;

import com.ims.admissions.domain.Enquiry;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnquiryJpaRepository extends JpaRepository<Enquiry, Long> {

  Optional<Enquiry> findByIdAndInstituteId(Long id, Long instituteId);

  @Query(
      """
      SELECT e FROM Enquiry e
      WHERE e.instituteId = :instituteId
        AND (:status IS NULL OR e.status = :status)
        AND (
          :q IS NULL OR :q = ''
          OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(e.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(e.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  Page<Enquiry> search(
      @Param("instituteId") Long instituteId,
      @Param("q") String q,
      @Param("status") String status,
      Pageable pageable);
}
