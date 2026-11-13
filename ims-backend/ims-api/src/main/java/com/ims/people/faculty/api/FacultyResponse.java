package com.ims.people.faculty.api;

import java.util.List;

public record FacultyResponse(
    Long id,
    String facultyCode,
    String firstName,
    String lastName,
    String phone,
    String email,
    String department,
    String status,
    List<AddressResponse> addresses) {

  public record AddressResponse(
      Long id,
      String line1,
      String line2,
      String city,
      String state,
      String postalCode,
      String country,
      boolean primaryAddress) {}
}
