package com.ims.finance.domain;

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
@Table(name = "financial_years")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class FinancialYear {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected FinancialYear() {}

  public static FinancialYear create(
      Long instituteId, String name, LocalDate startDate, LocalDate endDate, boolean active) {
    FinancialYear year = new FinancialYear();
    year.instituteId = instituteId;
    year.name = name.trim();
    year.startDate = startDate;
    year.endDate = endDate;
    year.active = active;
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

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public boolean isActive() {
    return active;
  }
}
