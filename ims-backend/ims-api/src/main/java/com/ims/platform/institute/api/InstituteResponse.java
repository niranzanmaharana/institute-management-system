package com.ims.platform.institute.api;

public record InstituteResponse(
    Long id,
    String code,
    String name,
    String status,
    String timezone,
    String mobile,
    String adminEmail,
    String website,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    String iconUrl) {}
