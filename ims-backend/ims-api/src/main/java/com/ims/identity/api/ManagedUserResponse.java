package com.ims.identity.api;

import java.time.Instant;
import java.util.List;

public record ManagedUserResponse(
    Long id,
    String username,
    String email,
    String status,
    List<String> roles,
    Instant lastLoginAt,
    Instant createdAt) {}
