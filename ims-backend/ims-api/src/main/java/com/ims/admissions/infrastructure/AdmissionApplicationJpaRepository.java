package com.ims.admissions.infrastructure;

import com.ims.admissions.domain.AdmissionApplication;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdmissionApplicationJpaRepository extends JpaRepository<AdmissionApplication, Long> {

  Optional<AdmissionApplication> findByIdAndInstituteId(Long id, Long instituteId);

  boolean existsByInstituteIdAndApplicationNoIgnoreCase(Long instituteId, String applicationNo);

  boolean existsByInstituteIdAndCourseIdAndPhoneAndStatus(
      Long instituteId, Long courseId, String phone, String status);

  boolean existsByInstituteIdAndCourseIdAndEmailIgnoreCaseAndStatus(
      Long instituteId, Long courseId, String email, String status);

  boolean existsByInstituteIdAndStudentIdAndCourseIdAndStatus(
      Long instituteId, Long studentId, Long courseId, String status);

  @Query(
      """
      SELECT a FROM AdmissionApplication a
      WHERE a.instituteId = :instituteId
        AND (:status IS NULL OR a.status = :status)
        AND (
          :q IS NULL OR :q = ''
          OR LOWER(a.applicationNo) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(a.applicantName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(a.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(COALESCE(a.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        )
      """)
  Page<AdmissionApplication> search(
      @Param("instituteId") Long instituteId,
      @Param("q") String q,
      @Param("status") String status,
      Pageable pageable);
}
