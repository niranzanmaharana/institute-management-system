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
@Table(name = "attendance_sessions")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class AttendanceSession {

  public static final String STATUS_DRAFT = "DRAFT";
  public static final String STATUS_SUBMITTED = "SUBMITTED";
  public static final String STATUS_LOCKED = "LOCKED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "session_date", nullable = false)
  private LocalDate sessionDate;

  @Column(name = "subject_id")
  private Long subjectId;

  @Column(nullable = false, length = 32)
  private String status = STATUS_DRAFT;

  @Column(name = "marked_by")
  private Long markedBy;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "unlocked_at")
  private Instant unlockedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected AttendanceSession() {}

  public static AttendanceSession create(
      Long instituteId, Long batchId, LocalDate sessionDate, Long subjectId, Long markedBy) {
    AttendanceSession row = new AttendanceSession();
    row.instituteId = instituteId;
    row.batchId = batchId;
    row.sessionDate = sessionDate;
    row.subjectId = subjectId;
    row.status = STATUS_DRAFT;
    row.markedBy = markedBy;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void requireEditable() {
    if (STATUS_LOCKED.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Session is LOCKED — unlock before editing");
    }
  }

  public void submit() {
    if (!STATUS_DRAFT.equals(status) && !STATUS_SUBMITTED.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only DRAFT sessions can be submitted");
    }
    this.status = STATUS_SUBMITTED;
    this.updatedAt = Instant.now();
  }

  public void lock() {
    if (!STATUS_SUBMITTED.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only SUBMITTED sessions can be locked");
    }
    Instant now = Instant.now();
    this.status = STATUS_LOCKED;
    this.lockedAt = now;
    this.updatedAt = now;
  }

  public void unlock() {
    if (!STATUS_LOCKED.equals(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only LOCKED sessions can be unlocked");
    }
    Instant now = Instant.now();
    this.status = STATUS_SUBMITTED;
    this.unlockedAt = now;
    this.updatedAt = now;
  }

  public void touchMarkedBy(Long userId) {
    this.markedBy = userId;
    this.updatedAt = Instant.now();
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

  public LocalDate getSessionDate() {
    return sessionDate;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public String getStatus() {
    return status;
  }

  public Long getMarkedBy() {
    return markedBy;
  }

  public Instant getLockedAt() {
    return lockedAt;
  }

  public Instant getUnlockedAt() {
    return unlockedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
