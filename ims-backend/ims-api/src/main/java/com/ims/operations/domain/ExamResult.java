package com.ims.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.Filter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "exam_results")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class ExamResult {

  public static final String STATUS_MARKS = "MARKS";
  public static final String STATUS_ABSENT = "ABSENT";
  public static final String STATUS_WITHHELD = "WITHHELD";

  public static final String PASS = "PASS";
  public static final String FAIL = "FAIL";

  /** Predefined letter grades (highest → lowest). */
  public static final List<String> GRADES =
      List.of("O", "A+", "A", "B+", "B", "C", "D", "F");

  private static final Set<String> RESULT_STATUSES =
      Set.of(STATUS_MARKS, STATUS_ABSENT, STATUS_WITHHELD);

  private static final Set<String> ALLOWED_GRADES = Set.copyOf(GRADES);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "exam_id", nullable = false)
  private Long examId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "result_status", nullable = false, length = 32)
  private String resultStatus = STATUS_MARKS;

  @Column(name = "total_marks", precision = 8, scale = 2)
  private BigDecimal totalMarks;

  @Column(name = "grace_marks", precision = 8, scale = 2)
  private BigDecimal graceMarks;

  @Column(name = "pass_fail", length = 16)
  private String passFail;

  @Column(length = 8)
  private String grade;

  @Column(length = 255)
  private String remarks;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ExamResult() {}

  public static ExamResult create(Long instituteId, Long examId, Long studentId) {
    ExamResult row = new ExamResult();
    row.instituteId = instituteId;
    row.examId = examId;
    row.studentId = studentId;
    row.resultStatus = STATUS_MARKS;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void apply(
      String resultStatus,
      BigDecimal totalMarks,
      BigDecimal graceMarks,
      String passFail,
      String grade,
      String remarks) {
    this.resultStatus = normalizeStatus(resultStatus);
    this.totalMarks = totalMarks;
    this.graceMarks = graceMarks;
    this.passFail = passFail;
    this.grade = normalizeGrade(grade);
    this.remarks = blankToNull(remarks);
    this.updatedAt = Instant.now();
  }

  private static String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      return STATUS_MARKS;
    }
    String normalized = status.trim().toUpperCase();
    if (!RESULT_STATUSES.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resultStatus");
    }
    return normalized;
  }

  private static String normalizeGrade(String grade) {
    if (grade == null || grade.isBlank()) {
      return null;
    }
    String normalized = grade.trim().toUpperCase();
    if (!ALLOWED_GRADES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid grade. Allowed: " + String.join(", ", GRADES));
    }
    return normalized;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
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

  public Long getStudentId() {
    return studentId;
  }

  public String getResultStatus() {
    return resultStatus;
  }

  public BigDecimal getTotalMarks() {
    return totalMarks;
  }

  public BigDecimal getGraceMarks() {
    return graceMarks;
  }

  public String getPassFail() {
    return passFail;
  }

  public String getGrade() {
    return grade;
  }

  public String getRemarks() {
    return remarks;
  }
}
