package com.ims.admissions.api;

import java.time.Instant;

public record EnquiryResponse(
    Long id,
    String name,
    String phone,
    String email,
    Long interestedCourseId,
    String status,
    String notes,
    Long convertedApplicationId,
    Instant createdAt) {}
