package com.ims.academic.infrastructure;

import com.ims.academic.domain.Course;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseJpaRepository extends JpaRepository<Course, Long> {
  boolean existsByInstituteIdAndCodeIgnoreCase(Long instituteId, String code);

  Optional<Course> findByIdAndInstituteIdAndDeletedAtIsNull(Long id, Long instituteId);

  @Query(
      """
      SELECT c FROM Course c
      WHERE c.instituteId = :instituteId
        AND c.deletedAt IS NULL
        AND (
          :q IS NULL OR :q = ''
          OR LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  Page<Course> search(
      @Param("instituteId") Long instituteId, @Param("q") String q, Pageable pageable);
}
