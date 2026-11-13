package com.ims.people.document.api;

import com.ims.people.document.domain.DocumentOwnerType;
import java.time.Instant;

public record PersonDocumentResponse(
    Long id,
    DocumentOwnerType ownerType,
    Long ownerId,
    String docType,
    String fileName,
    String contentType,
    Long fileSize,
    String checksum,
    String status,
    Long uploadedBy,
    Instant uploadedAt) {}
