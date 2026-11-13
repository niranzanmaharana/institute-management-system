package com.ims.people.document.application;

import com.ims.people.document.domain.DocumentOwnerType;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class DocumentRules {

  public static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("application/pdf", "image/jpeg", "image/png");
  public static final Set<String> DOC_TYPES =
      Set.of("ID_PROOF", "ADDRESS_PROOF", "PHOTO", "QUALIFICATION", "OTHER");

  private DocumentRules() {}

  public static String normalizeContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content type is required");
    }
    String normalized = contentType.trim().toLowerCase(Locale.ROOT);
    int semicolon = normalized.indexOf(';');
    if (semicolon >= 0) {
      normalized = normalized.substring(0, semicolon).trim();
    }
    if ("image/jpg".equals(normalized)) {
      normalized = "image/jpeg";
    }
    if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Allowed types are PDF, JPEG, and PNG");
    }
    return normalized;
  }

  public static String normalizeDocType(String docType) {
    if (docType == null || docType.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type is required");
    }
    String normalized = docType.trim().toUpperCase(Locale.ROOT);
    if (!DOC_TYPES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Document type must be ID_PROOF, ADDRESS_PROOF, PHOTO, QUALIFICATION, or OTHER");
    }
    return normalized;
  }

  public static void validateFileSize(long fileSize, long maxSizeBytes) {
    if (fileSize <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must be greater than 0");
    }
    if (fileSize > maxSizeBytes) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "File exceeds the maximum size of 5 MB");
    }
  }

  public static String sanitizeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is required");
    }
    String base = fileName.replace('\\', '/');
    int slash = base.lastIndexOf('/');
    if (slash >= 0) {
      base = base.substring(slash + 1);
    }
    if (base.isBlank() || ".".equals(base) || "..".equals(base)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is invalid");
    }
    String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
    if (cleaned.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is invalid");
    }
    if (cleaned.length() > 200) {
      cleaned = cleaned.substring(cleaned.length() - 200);
    }
    return cleaned;
  }

  public static void validatePhotoContentType(String docType, String contentType) {
    if ("PHOTO".equals(docType)
        && !"image/jpeg".equals(contentType)
        && !"image/png".equals(contentType)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Profile photo must be JPEG or PNG");
    }
  }

  public static void validateExtensionMatchesType(String fileName, String contentType) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    boolean ok =
        switch (contentType) {
          case "application/pdf" -> lower.endsWith(".pdf");
          case "image/jpeg" -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
          case "image/png" -> lower.endsWith(".png");
          default -> false;
        };
    if (!ok) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "File extension does not match the content type");
    }
  }

  public static String storageKey(
      long instituteId, DocumentOwnerType ownerType, long ownerId, String sanitizedFileName) {
    return "institutes/"
        + instituteId
        + "/"
        + ownerType.name().toLowerCase(Locale.ROOT)
        + "/"
        + ownerId
        + "/"
        + UUID.randomUUID()
        + "/"
        + sanitizedFileName;
  }

  public static void requireTenantPrefix(String storageKey, long instituteId) {
    String expected = "institutes/" + instituteId + "/";
    if (storageKey == null || !storageKey.startsWith(expected)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
  }

  public static String normalizeChecksum(String checksum) {
    if (checksum == null || checksum.isBlank()) {
      return null;
    }
    String hex = checksum.trim().toLowerCase(Locale.ROOT);
    if (!hex.matches("[a-f0-9]{64}")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Checksum must be a SHA-256 hex string");
    }
    return hex;
  }
}
