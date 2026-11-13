package com.ims.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "batch_faculty_assignments")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class BatchFacultyAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "faculty_id", nullable = false)
  private Long facultyId;

  @Column(name = "subject_id")
  private Long subjectId;

  @Column(nullable = false, length = 32)
  private String role = "TEACHER";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected BatchFacultyAssignment() {}

  public static BatchFacultyAssignment create(
      Long instituteId, Long batchId, Long facultyId, Long subjectId, String role) {
    BatchFacultyAssignment row = new BatchFacultyAssignment();
    row.instituteId = instituteId;
    row.batchId = batchId;
    row.facultyId = facultyId;
    row.subjectId = subjectId;
    row.role = role == null || role.isBlank() ? "TEACHER" : role.trim().toUpperCase();
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getFacultyId() {
    return facultyId;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public String getRole() {
    return role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
