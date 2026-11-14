package com.ims.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "exam_component_marks")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class ExamComponentMark {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "exam_result_id", nullable = false)
  private Long examResultId;

  @Column(name = "exam_component_id", nullable = false)
  private Long examComponentId;

  @Column(precision = 8, scale = 2)
  private BigDecimal marks;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ExamComponentMark() {}

  public static ExamComponentMark create(
      Long instituteId, Long examResultId, Long examComponentId, BigDecimal marks) {
    ExamComponentMark row = new ExamComponentMark();
    row.instituteId = instituteId;
    row.examResultId = examResultId;
    row.examComponentId = examComponentId;
    row.marks = marks;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void setMarks(BigDecimal marks) {
    this.marks = marks;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getExamResultId() {
    return examResultId;
  }

  public Long getExamComponentId() {
    return examComponentId;
  }

  public BigDecimal getMarks() {
    return marks;
  }
}
