package com.ims.documents.api;

import java.time.Instant;

public final class CertificateDtos {

  private CertificateDtos() {}

  public record CertificateTemplateResponse(String code, String name, boolean active) {}

  public record GeneratedDocumentResponse(
      Long id,
      String templateCode,
      Long studentId,
      Long enrollmentId,
      String fileName,
      String contentType,
      long fileSize,
      String status,
      Instant issuedAt,
      Long issuedByUserId) {}

  public record CertificateDownloadResponse(String url, long expiresInSeconds) {}
}
