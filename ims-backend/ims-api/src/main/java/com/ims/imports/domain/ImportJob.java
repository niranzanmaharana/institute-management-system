package com.ims.imports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "import_jobs")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class ImportJob {

  public static final String TYPE_MARKS = "MARKS";

  public static final String STATUS_QUEUED = "QUEUED";
  public static final String STATUS_RUNNING = "RUNNING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "job_type", nullable = false, length = 32)
  private String jobType;

  @Column(name = "exam_id")
  private Long examId;

  @Column(nullable = false, length = 32)
  private String status = STATUS_QUEUED;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "total_rows", nullable = false)
  private int totalRows;

  @Column(name = "success_count", nullable = false)
  private int successCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Lob
  @Column(name = "error_report_csv", columnDefinition = "MEDIUMTEXT")
  private String errorReportCsv;

  @Column(length = 512)
  private String message;

  @Lob
  @Column(name = "payload_csv", columnDefinition = "MEDIUMTEXT")
  private String payloadCsv;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected ImportJob() {}

  public static ImportJob createMarks(
      Long instituteId, Long examId, String fileName, String payloadCsv, Long createdByUserId) {
    ImportJob job = new ImportJob();
    job.instituteId = instituteId;
    job.jobType = TYPE_MARKS;
    job.examId = examId;
    job.status = STATUS_QUEUED;
    job.fileName = fileName;
    job.payloadCsv = payloadCsv;
    job.createdByUserId = createdByUserId;
    job.createdAt = Instant.now();
    return job;
  }

  public void markRunning() {
    this.status = STATUS_RUNNING;
    this.startedAt = Instant.now();
  }

  public void markCompleted(int totalRows, int successCount, int failedCount, String errorCsv) {
    this.status = STATUS_COMPLETED;
    this.totalRows = totalRows;
    this.successCount = successCount;
    this.failedCount = failedCount;
    this.errorReportCsv = errorCsv;
    this.completedAt = Instant.now();
    this.payloadCsv = null;
  }

  public void markFailed(String message) {
    this.status = STATUS_FAILED;
    this.message = message == null ? "Import failed" : message;
    if (this.message.length() > 512) {
      this.message = this.message.substring(0, 512);
    }
    this.completedAt = Instant.now();
    this.payloadCsv = null;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getJobType() {
    return jobType;
  }

  public Long getExamId() {
    return examId;
  }

  public String getStatus() {
    return status;
  }

  public String getFileName() {
    return fileName;
  }

  public int getTotalRows() {
    return totalRows;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public String getErrorReportCsv() {
    return errorReportCsv;
  }

  public String getMessage() {
    return message;
  }

  public String getPayloadCsv() {
    return payloadCsv;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
