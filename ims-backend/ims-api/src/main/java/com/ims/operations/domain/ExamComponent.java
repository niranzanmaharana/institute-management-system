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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "exam_components")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class ExamComponent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "exam_id", nullable = false)
  private Long examId;

  @Column(name = "subject_id")
  private Long subjectId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "max_marks", nullable = false, precision = 8, scale = 2)
  private BigDecimal maxMarks;

  @Column(name = "pass_marks", nullable = false, precision = 8, scale = 2)
  private BigDecimal passMarks;

  @Column(precision = 5, scale = 2)
  private BigDecimal weightage;

  @Column(nullable = false)
  private int seq = 1;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected ExamComponent() {}

  public static ExamComponent create(
      Long instituteId,
      Long examId,
      Long subjectId,
      String code,
      String name,
      BigDecimal maxMarks,
      BigDecimal passMarks,
      BigDecimal weightage,
      int seq) {
    ExamComponent row = new ExamComponent();
    row.instituteId = instituteId;
    row.examId = examId;
    row.subjectId = subjectId;
    row.code = requireCode(code);
    row.name = requireName(name);
    row.maxMarks = requireMarks(maxMarks, "maxMarks");
    row.passMarks = requireMarks(passMarks, "passMarks");
    if (row.passMarks.compareTo(row.maxMarks) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passMarks cannot exceed maxMarks");
    }
    row.weightage = weightage;
    row.seq = Math.max(seq, 1);
    row.createdAt = Instant.now();
    return row;
  }

  private static String requireCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component code is required");
    }
    return code.trim().toUpperCase();
  }

  private static String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component name is required");
    }
    return name.trim();
  }

  private static BigDecimal requireMarks(BigDecimal value, String field) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be >= 0");
    }
    return value;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getExamId() {
    return examId;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getMaxMarks() {
    return maxMarks;
  }

  public BigDecimal getPassMarks() {
    return passMarks;
  }

  public BigDecimal getWeightage() {
    return weightage;
  }

  public int getSeq() {
    return seq;
  }
}
