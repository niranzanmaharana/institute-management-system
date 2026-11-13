package com.ims.people.staff.api;

public record StaffResponse(
    Long id,
    String staffCode,
    String firstName,
    String lastName,
    String phone,
    String email,
    String designation,
    String status) {}
