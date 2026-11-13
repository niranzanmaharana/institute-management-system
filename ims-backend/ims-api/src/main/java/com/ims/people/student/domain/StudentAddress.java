package com.ims.people.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "student_addresses")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class StudentAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(nullable = false)
  private String line1;

  @Column
  private String line2;

  @Column(nullable = false, length = 64)
  private String city;

  @Column(length = 64)
  private String state;

  @Column(name = "postal_code", length = 16)
  private String postalCode;

  @Column(nullable = false, length = 64)
  private String country = "India";

  @Column(name = "is_primary", nullable = false)
  private boolean primaryAddress = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected StudentAddress() {}

  public static StudentAddress create(
      Long instituteId,
      Long studentId,
      String line1,
      String line2,
      String city,
      String state,
      String postalCode,
      String country,
      boolean primaryAddress) {
    StudentAddress address = new StudentAddress();
    address.instituteId = instituteId;
    address.studentId = studentId;
    address.line1 = line1.trim();
    address.line2 = blankToNull(line2);
    address.city = city.trim();
    address.state = blankToNull(state);
    address.postalCode = blankToNull(postalCode);
    address.country =
        country == null || country.isBlank() ? "India" : country.trim();
    address.primaryAddress = primaryAddress;
    Instant now = Instant.now();
    address.createdAt = now;
    address.updatedAt = now;
    return address;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getStudentId() {
    return studentId;
  }

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountry() {
    return country;
  }

  public boolean isPrimaryAddress() {
    return primaryAddress;
  }
}
