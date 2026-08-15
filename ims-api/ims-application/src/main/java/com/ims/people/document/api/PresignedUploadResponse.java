package com.ims.people.document.api;

import java.time.Instant;
import java.util.Map;

public record PresignedUploadResponse(
    PersonDocumentResponse document,
    String uploadUrl,
    Instant expiresAt,
    Map<String, String> headers) {}
