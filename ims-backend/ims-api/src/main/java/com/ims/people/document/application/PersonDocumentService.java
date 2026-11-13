package com.ims.people.document.application;

import com.ims.common.tenancy.TenantContext;
import com.ims.identity.security.ImsUserPrincipal;
import com.ims.people.document.api.PersonDocumentResponse;
import com.ims.people.document.api.PresignedDownloadResponse;
import com.ims.people.document.api.PresignedUploadResponse;
import com.ims.people.document.domain.DocumentOwnerType;
import com.ims.people.document.domain.PersonDocument;
import com.ims.people.document.infrastructure.PersonDocumentJpaRepository;
import com.ims.people.faculty.infrastructure.FacultyJpaRepository;
import com.ims.people.staff.infrastructure.StaffJpaRepository;
import com.ims.people.student.infrastructure.StudentJpaRepository;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.storage.ObjectStorage;
import com.ims.platform.storage.StorageProperties;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonDocumentService {

  private static final Logger log = LoggerFactory.getLogger(PersonDocumentService.class);

  private final PersonDocumentJpaRepository documentRepository;
  private final StudentJpaRepository studentJpaRepository;
  private final FacultyJpaRepository facultyJpaRepository;
  private final StaffJpaRepository staffJpaRepository;
  private final ObjectStorage objectStorage;
  private final StorageProperties storageProperties;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;

  public PersonDocumentService(
      PersonDocumentJpaRepository documentRepository,
      StudentJpaRepository studentJpaRepository,
      FacultyJpaRepository facultyJpaRepository,
      StaffJpaRepository staffJpaRepository,
      ObjectStorage objectStorage,
      StorageProperties storageProperties,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService) {
    this.documentRepository = documentRepository;
    this.studentJpaRepository = studentJpaRepository;
    this.facultyJpaRepository = facultyJpaRepository;
    this.staffJpaRepository = staffJpaRepository;
    this.objectStorage = objectStorage;
    this.storageProperties = storageProperties;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
  }

  public record InitUploadCommand(
      String docType, String fileName, String contentType, long fileSize) {}

  @Transactional(readOnly = true)
  public List<PersonDocumentResponse> list(DocumentOwnerType ownerType, Long ownerId) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    requireOwner(ownerType, ownerId, instituteId);
    return documentRepository
        .findByInstituteIdAndOwnerTypeAndOwnerIdAndStatusOrderByUploadedAtDesc(
            instituteId, ownerType, ownerId, PersonDocument.STATUS_ACTIVE)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public PresignedUploadResponse initUpload(
      DocumentOwnerType ownerType, Long ownerId, InitUploadCommand command) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    requireOwner(ownerType, ownerId, instituteId);

    String contentType = DocumentRules.normalizeContentType(command.contentType());
    String docType = DocumentRules.normalizeDocType(command.docType());
    DocumentRules.validateFileSize(command.fileSize(), storageProperties.maxSizeBytes());
    String fileName = DocumentRules.sanitizeFileName(command.fileName());
    DocumentRules.validateExtensionMatchesType(fileName, contentType);
    DocumentRules.validatePhotoContentType(docType, contentType);

    String storageKey = DocumentRules.storageKey(instituteId, ownerType, ownerId, fileName);
    PersonDocument pending =
        documentRepository.save(
            PersonDocument.pending(
                instituteId,
                ownerType,
                ownerId,
                docType,
                fileName,
                storageKey,
                contentType,
                command.fileSize(),
                currentUserId()));

    Duration expiry = Duration.ofMinutes(storageProperties.presignExpiryMinutes());
    String uploadUrl =
        objectStorage.presignPut(storageKey, contentType, command.fileSize(), expiry);
    Instant expiresAt = Instant.now().plus(expiry);
    log.info(
        "Document upload initialized id={} owner={}={} instituteId={}",
        pending.getId(),
        ownerType,
        ownerId,
        instituteId);
    return new PresignedUploadResponse(
        toResponse(pending), uploadUrl, expiresAt, Map.of("Content-Type", contentType));
  }

  @Transactional
  public PersonDocumentResponse complete(
      DocumentOwnerType ownerType, Long ownerId, Long documentId, String checksum) {
    PersonDocument document = requireOwnedDocument(ownerType, ownerId, documentId);
    if (!document.isPending()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Document upload is not pending");
    }
    DocumentRules.requireTenantPrefix(document.getStorageKey(), document.getInstituteId());
    ObjectStorage.StoredObjectMeta meta =
        objectStorage
            .head(document.getStorageKey())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Upload not found in storage. Complete after PUT."));
    if (meta.contentLength() > 0 && meta.contentLength() != document.getFileSize()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Uploaded file size does not match the declared size");
    }
    document.markActive(DocumentRules.normalizeChecksum(checksum));
    documentRepository.save(document);
    auditService.record(
        document.getInstituteId(),
        "people",
        "PersonDocument",
        document.getId(),
        "DOCUMENT_UPLOADED",
        null,
        Map.of(
            "ownerType",
            document.getOwnerType().name(),
            "ownerId",
            document.getOwnerId(),
            "docType",
            document.getDocType(),
            "fileName",
            document.getFileName(),
            "fileSize",
            document.getFileSize()),
        null);
    return toResponse(document);
  }

  @Transactional(readOnly = true)
  public PresignedDownloadResponse download(
      DocumentOwnerType ownerType, Long ownerId, Long documentId) {
    PersonDocument document = requireOwnedDocument(ownerType, ownerId, documentId);
    if (!document.isActive()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
    DocumentRules.requireTenantPrefix(document.getStorageKey(), document.getInstituteId());
    return presign(document, true);
  }

  @Transactional(readOnly = true)
  public PresignedDownloadResponse latestPhoto(DocumentOwnerType ownerType, Long ownerId) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    requireOwner(ownerType, ownerId, instituteId);
    PersonDocument photo =
        documentRepository
            .findFirstByInstituteIdAndOwnerTypeAndOwnerIdAndDocTypeAndStatusOrderByUploadedAtDesc(
                instituteId, ownerType, ownerId, "PHOTO", PersonDocument.STATUS_ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
    if (photo.getContentType() == null || !photo.getContentType().startsWith("image/")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
    }
    DocumentRules.requireTenantPrefix(photo.getStorageKey(), photo.getInstituteId());
    return presign(photo, false);
  }

  private PresignedDownloadResponse presign(PersonDocument document, boolean attachment) {
    Duration expiry = Duration.ofMinutes(storageProperties.presignExpiryMinutes());
    String downloadUrl =
        objectStorage.presignGet(
            document.getStorageKey(),
            document.getFileName(),
            document.getContentType(),
            expiry,
            attachment);
    return new PresignedDownloadResponse(
        toResponse(document), downloadUrl, Instant.now().plus(expiry));
  }

  @Transactional
  public PersonDocumentResponse delete(
      DocumentOwnerType ownerType, Long ownerId, Long documentId) {
    PersonDocument document = requireOwnedDocument(ownerType, ownerId, documentId);
    if (PersonDocument.STATUS_DELETED.equals(document.getStatus())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
    document.markDeleted();
    documentRepository.save(document);
    auditService.record(
        document.getInstituteId(),
        "people",
        "PersonDocument",
        document.getId(),
        "DOCUMENT_DELETED",
        Map.of("fileName", document.getFileName()),
        Map.of("status", PersonDocument.STATUS_DELETED),
        null);
    return toResponse(document);
  }

  private PersonDocument requireOwnedDocument(
      DocumentOwnerType ownerType, Long ownerId, Long documentId) {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    requireOwner(ownerType, ownerId, instituteId);
    PersonDocument document =
        documentRepository
            .findByIdAndInstituteId(documentId, instituteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    if (document.getOwnerType() != ownerType || !document.getOwnerId().equals(ownerId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
    return document;
  }

  private void requireOwner(DocumentOwnerType ownerType, Long ownerId, long instituteId) {
    boolean exists =
        switch (ownerType) {
          case STUDENT ->
              studentJpaRepository
                  .findByIdAndInstituteIdAndDeletedAtIsNull(ownerId, instituteId)
                  .isPresent();
          case FACULTY ->
              facultyJpaRepository
                  .findByIdAndInstituteIdAndDeletedAtIsNull(ownerId, instituteId)
                  .isPresent();
          case STAFF ->
              staffJpaRepository
                  .findByIdAndInstituteIdAndDeletedAtIsNull(ownerId, instituteId)
                  .isPresent();
        };
    if (!exists) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, ownerType.name().toLowerCase(Locale.ROOT) + " not found");
    }
  }

  private PersonDocumentResponse toResponse(PersonDocument document) {
    return new PersonDocumentResponse(
        document.getId(),
        document.getOwnerType(),
        document.getOwnerId(),
        document.getDocType(),
        document.getFileName(),
        document.getContentType(),
        document.getFileSize(),
        document.getChecksum(),
        document.getStatus(),
        document.getUploadedBy(),
        document.getUploadedAt());
  }

  private Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof ImsUserPrincipal principal) {
      return principal.getUserId();
    }
    return null;
  }
}
