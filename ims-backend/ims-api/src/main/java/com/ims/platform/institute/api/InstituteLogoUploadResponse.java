package com.ims.platform.institute.api;

import java.time.Instant;
import java.util.Map;

public record InstituteLogoUploadResponse(
    String storageKey, String uploadUrl, Instant expiresAt, Map<String, String> headers) {}
