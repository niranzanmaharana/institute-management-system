package com.ims.admissions.api;

import java.time.Instant;

public record AdmissionResponse(
    Long id,
    String applicationNo,
    Long enquiryId,
    Long courseId,
    String applicantName,
    String phone,
    String email,
    String status,
    Long studentId,
    Long decidedBy,
    String decisionReason,
    Instant decidedAt,
    Instant createdAt) {}
