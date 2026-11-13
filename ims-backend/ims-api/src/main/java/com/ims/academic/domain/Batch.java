package com.ims.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "batches")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Batch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "academic_year_id", nullable = false)
  private Long academicYearId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(nullable = false)
  private int capacity;

  @Column(nullable = false, length = 32)
  private String status = "OPEN";

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Batch() {}

  public static Batch create(
      Long instituteId,
      Long courseId,
      Long academicYearId,
      String code,
      String name,
      int capacity,
      LocalDate startDate,
      LocalDate endDate) {
    Batch batch = new Batch();
    batch.instituteId = instituteId;
    batch.courseId = courseId;
    batch.academicYearId = academicYearId;
    batch.code = code.trim().toUpperCase();
    batch.name = name.trim();
    batch.capacity = capacity;
    batch.status = "OPEN";
    batch.startDate = startDate;
    batch.endDate = endDate;
    Instant now = Instant.now();
    batch.createdAt = now;
    batch.updatedAt = now;
    return batch;
  }

  public void update(String name, int capacity, String status, LocalDate startDate, LocalDate endDate) {
    this.name = name.trim();
    this.capacity = capacity;
    if (status != null && !status.isBlank()) {
      this.status = status.trim().toUpperCase();
    }
    this.startDate = startDate;
    this.endDate = endDate;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public Long getAcademicYearId() {
    return academicYearId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getCapacity() {
    return capacity;
  }

  public String getStatus() {
    return status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }
}
