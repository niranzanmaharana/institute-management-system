package com.ims.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "enrollments")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Enrollment {

  public static final String STATUS_APPLIED = "APPLIED";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_SUSPENDED = "SUSPENDED";
  public static final String STATUS_WITHDRAWN = "WITHDRAWN";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "fee_plan_id", nullable = false)
  private Long feePlanId;

  @Column(nullable = false, length = 32)
  private String status = STATUS_APPLIED;

  @Column(name = "activated_at")
  private Instant activatedAt;

  @Column(name = "suspended_at")
  private Instant suspendedAt;

  @Column(name = "withdrawn_at")
  private Instant withdrawnAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Enrollment() {}

  public static Enrollment create(
      Long instituteId, Long studentId, Long batchId, Long courseId, Long feePlanId) {
    Enrollment enrollment = new Enrollment();
    enrollment.instituteId = instituteId;
    enrollment.studentId = studentId;
    enrollment.batchId = batchId;
    enrollment.courseId = courseId;
    enrollment.feePlanId = feePlanId;
    enrollment.status = STATUS_APPLIED;
    Instant now = Instant.now();
    enrollment.createdAt = now;
    enrollment.updatedAt = now;
    return enrollment;
  }

  public void activate() {
    requireStatus(STATUS_APPLIED, "Only APPLIED enrollments can be activated");
    Instant now = Instant.now();
    this.status = STATUS_ACTIVE;
    this.activatedAt = now;
    this.updatedAt = now;
  }

  public void cancel() {
    requireStatus(STATUS_APPLIED, "Only APPLIED enrollments can be cancelled");
    Instant now = Instant.now();
    this.status = STATUS_CANCELLED;
    this.cancelledAt = now;
    this.updatedAt = now;
  }

  public void suspend() {
    requireStatus(STATUS_ACTIVE, "Only ACTIVE enrollments can be suspended");
    Instant now = Instant.now();
    this.status = STATUS_SUSPENDED;
    this.suspendedAt = now;
    this.updatedAt = now;
  }

  public void resume() {
    requireStatus(STATUS_SUSPENDED, "Only SUSPENDED enrollments can be resumed");
    Instant now = Instant.now();
    this.status = STATUS_ACTIVE;
    this.suspendedAt = null;
    this.updatedAt = now;
  }

  public void withdraw() {
    if (!STATUS_ACTIVE.equals(status) && !STATUS_SUSPENDED.equals(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only ACTIVE or SUSPENDED enrollments can be withdrawn");
    }
    Instant now = Instant.now();
    this.status = STATUS_WITHDRAWN;
    this.withdrawnAt = now;
    this.updatedAt = now;
  }

  public void complete() {
    requireStatus(STATUS_ACTIVE, "Only ACTIVE enrollments can be completed");
    Instant now = Instant.now();
    this.status = STATUS_COMPLETED;
    this.completedAt = now;
    this.updatedAt = now;
  }

  public void transferTo(Long newBatchId, Long newFeePlanId) {
    requireStatus(STATUS_ACTIVE, "Only ACTIVE enrollments can be transferred");
    this.batchId = newBatchId;
    if (newFeePlanId != null) {
      this.feePlanId = newFeePlanId;
    }
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
  }

  private void requireStatus(String expected, String message) {
    if (!expected.equals(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getStudentId() {
    return studentId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public Long getFeePlanId() {
    return feePlanId;
  }

  public String getStatus() {
    return status;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  public Instant getSuspendedAt() {
    return suspendedAt;
  }

  public Instant getWithdrawnAt() {
    return withdrawnAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
