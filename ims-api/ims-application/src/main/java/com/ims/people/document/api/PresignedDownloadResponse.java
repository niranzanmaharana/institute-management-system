package com.ims.people.document.api;

import java.time.Instant;

public record PresignedDownloadResponse(
    PersonDocumentResponse document, String downloadUrl, Instant expiresAt) {}
