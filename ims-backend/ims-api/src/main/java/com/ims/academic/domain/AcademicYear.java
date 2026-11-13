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
@Table(name = "academic_years")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class AcademicYear {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected AcademicYear() {}

  public static AcademicYear create(
      Long instituteId, String code, String name, LocalDate startDate, LocalDate endDate) {
    AcademicYear year = new AcademicYear();
    year.instituteId = instituteId;
    year.code = code.trim().toUpperCase();
    year.name = name.trim();
    year.startDate = startDate;
    year.endDate = endDate;
    year.status = "ACTIVE";
    Instant now = Instant.now();
    year.createdAt = now;
    year.updatedAt = now;
    return year;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getStatus() {
    return status;
  }
}
