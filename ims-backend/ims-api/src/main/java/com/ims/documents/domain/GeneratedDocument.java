package com.ims.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "generated_documents")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class GeneratedDocument {

  public static final String STATUS_ACTIVE = "ACTIVE";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "template_code", nullable = false, length = 32)
  private String templateCode;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "storage_key", nullable = false, length = 512)
  private String storageKey;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 128)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(nullable = false, length = 32)
  private String status = STATUS_ACTIVE;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "issued_by_user_id")
  private Long issuedByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected GeneratedDocument() {}

  public static GeneratedDocument create(
      Long instituteId,
      String templateCode,
      Long studentId,
      Long enrollmentId,
      String storageKey,
      String fileName,
      String contentType,
      long fileSize,
      Long issuedByUserId) {
    GeneratedDocument row = new GeneratedDocument();
    row.instituteId = instituteId;
    row.templateCode = templateCode;
    row.studentId = studentId;
    row.enrollmentId = enrollmentId;
    row.storageKey = storageKey;
    row.fileName = fileName;
    row.contentType = contentType;
    row.fileSize = fileSize;
    row.status = STATUS_ACTIVE;
    Instant now = Instant.now();
    row.issuedAt = now;
    row.issuedByUserId = issuedByUserId;
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public Long getStudentId() {
    return studentId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getStatus() {
    return status;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Long getIssuedByUserId() {
    return issuedByUserId;
  }
}
