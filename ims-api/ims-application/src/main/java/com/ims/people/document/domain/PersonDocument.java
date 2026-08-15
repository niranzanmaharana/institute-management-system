package com.ims.people.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "person_documents")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class PersonDocument {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_DELETED = "DELETED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", nullable = false, length = 16)
  private DocumentOwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  private Long ownerId;

  @Column(name = "doc_type", nullable = false, length = 64)
  private String docType;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "storage_key", nullable = false, length = 512)
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 128)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(length = 128)
  private String checksum;

  @Column(nullable = false, length = 32)
  private String status = STATUS_PENDING;

  @Column(name = "uploaded_by")
  private Long uploadedBy;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt = Instant.now();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected PersonDocument() {}

  public static PersonDocument pending(
      Long instituteId,
      DocumentOwnerType ownerType,
      Long ownerId,
      String docType,
      String fileName,
      String storageKey,
      String contentType,
      Long fileSize,
      Long uploadedBy) {
    PersonDocument row = new PersonDocument();
    row.instituteId = instituteId;
    row.ownerType = ownerType;
    row.ownerId = ownerId;
    row.docType = docType;
    row.fileName = fileName;
    row.storageKey = storageKey;
    row.contentType = contentType;
    row.fileSize = fileSize;
    row.status = STATUS_PENDING;
    row.uploadedBy = uploadedBy;
    Instant now = Instant.now();
    row.uploadedAt = now;
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void markActive(String checksum) {
    this.status = STATUS_ACTIVE;
    this.checksum = checksum;
    this.uploadedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void markDeleted() {
    this.status = STATUS_DELETED;
    this.updatedAt = Instant.now();
  }

  public boolean isPending() {
    return STATUS_PENDING.equals(status);
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public DocumentOwnerType getOwnerType() {
    return ownerType;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public String getDocType() {
    return docType;
  }

  public String getFileName() {
    return fileName;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public String getChecksum() {
    return checksum;
  }

  public String getStatus() {
    return status;
  }

  public Long getUploadedBy() {
    return uploadedBy;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }
}
