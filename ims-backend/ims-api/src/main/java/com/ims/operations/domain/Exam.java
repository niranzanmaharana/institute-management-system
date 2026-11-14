package com.ims.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "exams")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Exam {

  public static final String STATUS_SCHEDULED = "SCHEDULED";
  public static final String STATUS_DRAFT_MARKS = "DRAFT_MARKS";
  public static final String STATUS_PUBLISHED = "PUBLISHED";
  public static final String STATUS_LOCKED = "LOCKED";
  public static final String STATUS_CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "exam_date", nullable = false)
  private LocalDate examDate;

  @Column(nullable = false, length = 32)
  private String status = STATUS_SCHEDULED;

  @Column(name = "original_exam_id")
  private Long originalExamId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "locked_at")
  private Instant lockedAt;

  protected Exam() {}

  public static Exam create(
      Long instituteId, Long batchId, String name, LocalDate examDate, Long originalExamId) {
    Exam exam = new Exam();
    exam.instituteId = instituteId;
    exam.batchId = batchId;
    exam.name = requireName(name);
    exam.examDate = requireDate(examDate);
    exam.status = STATUS_SCHEDULED;
    exam.originalExamId = originalExamId;
    Instant now = Instant.now();
    exam.createdAt = now;
    exam.updatedAt = now;
    return exam;
  }

  public void update(String name, LocalDate examDate) {
    requireScheduled();
    this.name = requireName(name);
    this.examDate = requireDate(examDate);
    this.updatedAt = Instant.now();
  }

  public void openMarks() {
    if (!STATUS_SCHEDULED.equals(status) && !STATUS_DRAFT_MARKS.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only SCHEDULED exams can open for marks entry");
    }
    this.status = STATUS_DRAFT_MARKS;
    this.updatedAt = Instant.now();
  }

  public void requireMarksEditable() {
    if (!STATUS_DRAFT_MARKS.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Marks can only be edited in DRAFT_MARKS status");
    }
  }

  public void publish() {
    if (!STATUS_DRAFT_MARKS.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only DRAFT_MARKS exams can be published");
    }
    Instant now = Instant.now();
    this.status = STATUS_PUBLISHED;
    this.publishedAt = now;
    this.updatedAt = now;
  }

  public void lock() {
    if (!STATUS_PUBLISHED.equals(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PUBLISHED exams can be locked");
    }
    Instant now = Instant.now();
    this.status = STATUS_LOCKED;
    this.lockedAt = now;
    this.updatedAt = now;
  }

  public void unlock() {
    if (!STATUS_PUBLISHED.equals(status) && !STATUS_LOCKED.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only PUBLISHED or LOCKED exams can be unlocked");
    }
    this.status = STATUS_DRAFT_MARKS;
    this.lockedAt = null;
    this.updatedAt = Instant.now();
  }

  public void cancel() {
    if (STATUS_LOCKED.equals(status) || STATUS_CANCELLED.equals(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel locked/cancelled exam");
    }
    this.status = STATUS_CANCELLED;
    this.updatedAt = Instant.now();
  }

  public boolean isVisibleToStudents() {
    return STATUS_PUBLISHED.equals(status) || STATUS_LOCKED.equals(status);
  }

  private void requireScheduled() {
    if (!STATUS_SCHEDULED.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Exam details can only be edited while SCHEDULED");
    }
  }

  private static String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam name is required");
    }
    return name.trim();
  }

  private static LocalDate requireDate(LocalDate examDate) {
    if (examDate == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "examDate is required");
    }
    return examDate;
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

  public String getName() {
    return name;
  }

  public LocalDate getExamDate() {
    return examDate;
  }

  public String getStatus() {
    return status;
  }

  public Long getOriginalExamId() {
    return originalExamId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getLockedAt() {
    return lockedAt;
  }
}
