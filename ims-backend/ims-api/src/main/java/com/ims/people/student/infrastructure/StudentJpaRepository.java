package com.ims.people.student.infrastructure;

import com.ims.people.student.domain.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentJpaRepository extends JpaRepository<Student, Long> {

  boolean existsByInstituteIdAndStudentCodeIgnoreCase(Long instituteId, String studentCode);

  Optional<Student> findByIdAndInstituteIdAndDeletedAtIsNull(Long id, Long instituteId);

  @Query(
      """
      SELECT s FROM Student s
      WHERE s.instituteId = :instituteId
        AND (
          (:status IS NULL AND s.deletedAt IS NULL)
          OR (:status = 'ACTIVE' AND s.status = 'ACTIVE' AND s.deletedAt IS NULL)
          OR (:status = 'INACTIVE' AND s.status = 'INACTIVE')
        )
        AND (
          :q IS NULL OR :q = ''
          OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  Page<Student> search(
      @Param("instituteId") Long instituteId,
      @Param("q") String q,
      @Param("status") String status,
      Pageable pageable);
}
