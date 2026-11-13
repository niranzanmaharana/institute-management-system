package com.ims.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "admission_applications")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class AdmissionApplication {

  public static final String STATUS_SUBMITTED = "SUBMITTED";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";
  public static final String STATUS_CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "application_no", nullable = false, length = 32)
  private String applicationNo;

  @Column(name = "enquiry_id")
  private Long enquiryId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "applicant_name", nullable = false, length = 128)
  private String applicantName;

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(nullable = false, length = 32)
  private String status = STATUS_SUBMITTED;

  @Column(name = "student_id")
  private Long studentId;

  @Column(name = "decided_by")
  private Long decidedBy;

  @Column(name = "decision_reason", length = 512)
  private String decisionReason;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected AdmissionApplication() {}

  public static AdmissionApplication create(
      Long instituteId,
      String applicationNo,
      Long courseId,
      String applicantName,
      String phone,
      String email,
      Long enquiryId) {
    AdmissionApplication app = new AdmissionApplication();
    app.instituteId = instituteId;
    app.applicationNo = applicationNo.trim().toUpperCase();
    app.enquiryId = enquiryId;
    app.courseId = courseId;
    app.applicantName = applicantName.trim();
    app.phone = blankToNull(phone);
    app.email = blankToNull(email);
    app.status = STATUS_SUBMITTED;
    Instant now = Instant.now();
    app.createdAt = now;
    app.updatedAt = now;
    return app;
  }

  public boolean isDecided() {
    return STATUS_APPROVED.equals(status)
        || STATUS_REJECTED.equals(status)
        || STATUS_CANCELLED.equals(status);
  }

  public boolean isOpen() {
    return STATUS_SUBMITTED.equals(status);
  }

  public void approve(Long studentId, Long decidedBy, String reason) {
    requireOpen();
    this.status = STATUS_APPROVED;
    this.studentId = studentId;
    this.decidedBy = decidedBy;
    this.decisionReason = blankToNull(reason);
    this.decidedAt = Instant.now();
    this.updatedAt = this.decidedAt;
  }

  public void reject(Long decidedBy, String reason) {
    requireOpen();
    this.status = STATUS_REJECTED;
    this.decidedBy = decidedBy;
    this.decisionReason = reason.trim();
    this.decidedAt = Instant.now();
    this.updatedAt = this.decidedAt;
  }

  public void cancel(Long decidedBy, String reason) {
    requireOpen();
    this.status = STATUS_CANCELLED;
    this.decidedBy = decidedBy;
    this.decisionReason = reason.trim();
    this.decidedAt = Instant.now();
    this.updatedAt = this.decidedAt;
  }

  private void requireOpen() {
    if (!isOpen()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, "Admission is not open for this action");
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getApplicationNo() {
    return applicationNo;
  }

  public Long getEnquiryId() {
    return enquiryId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public String getApplicantName() {
    return applicantName;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getStatus() {
    return status;
  }

  public Long getStudentId() {
    return studentId;
  }

  public Long getDecidedBy() {
    return decidedBy;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
