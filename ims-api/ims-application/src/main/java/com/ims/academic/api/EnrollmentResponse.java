package com.ims.academic.api;

import java.time.Instant;

public record EnrollmentResponse(
    Long id,
    Long studentId,
    Long batchId,
    Long courseId,
    Long feePlanId,
    String status,
    Instant activatedAt,
    Instant suspendedAt,
    Instant withdrawnAt,
    Instant completedAt,
    Instant cancelledAt,
    Instant createdAt) {}
