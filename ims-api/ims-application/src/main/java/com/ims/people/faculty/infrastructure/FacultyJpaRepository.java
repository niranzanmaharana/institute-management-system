package com.ims.people.faculty.infrastructure;

import com.ims.people.faculty.domain.Faculty;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacultyJpaRepository extends JpaRepository<Faculty, Long> {

  boolean existsByInstituteIdAndFacultyCodeIgnoreCase(Long instituteId, String facultyCode);

  Optional<Faculty> findByIdAndInstituteIdAndDeletedAtIsNull(Long id, Long instituteId);

  @Query(
      """
      SELECT f FROM Faculty f
      WHERE f.instituteId = :instituteId
        AND (
          (:status IS NULL AND f.deletedAt IS NULL)
          OR (:status = 'ACTIVE' AND f.status = 'ACTIVE' AND f.deletedAt IS NULL)
          OR (:status = 'INACTIVE' AND f.status = 'INACTIVE')
        )
        AND (
          :q IS NULL OR :q = ''
          OR LOWER(f.facultyCode) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(f.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(f.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(f.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(f.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(f.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  Page<Faculty> search(
      @Param("instituteId") Long instituteId,
      @Param("q") String q,
      @Param("status") String status,
      Pageable pageable);
}
