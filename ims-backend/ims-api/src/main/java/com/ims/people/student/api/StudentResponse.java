package com.ims.people.student.api;

import java.util.List;

public record StudentResponse(
    Long id,
    String studentCode,
    String firstName,
    String lastName,
    String phone,
    String email,
    String status,
    List<GuardianLinkResponse> guardians,
    List<AddressResponse> addresses) {

  public record GuardianLinkResponse(
      Long id, String name, String phone, String email, String relation, boolean primaryGuardian) {}

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
