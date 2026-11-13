package com.ims.people.staff.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "staff")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Staff {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "staff_code", nullable = false, length = 32)
  private String staffCode;

  @Column(name = "first_name", nullable = false, length = 64)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 64)
  private String lastName;

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(length = 128)
  private String designation;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Staff() {}

  public static Staff create(
      Long instituteId,
      String staffCode,
      String firstName,
      String lastName,
      String phone,
      String email,
      String designation) {
    Staff staff = new Staff();
    staff.instituteId = instituteId;
    staff.staffCode = staffCode.trim().toUpperCase();
    staff.firstName = firstName.trim();
    staff.lastName = lastName.trim();
    staff.phone = blankToNull(phone);
    staff.email = blankToNull(email);
    staff.designation = blankToNull(designation);
    staff.status = "ACTIVE";
    Instant now = Instant.now();
    staff.createdAt = now;
    staff.updatedAt = now;
    return staff;
  }

  public void update(
      String firstName, String lastName, String phone, String email, String designation) {
    this.firstName = firstName.trim();
    this.lastName = lastName.trim();
    this.phone = blankToNull(phone);
    this.email = blankToNull(email);
    this.designation = blankToNull(designation);
    this.updatedAt = Instant.now();
  }

  public void deactivate() {
    this.status = "INACTIVE";
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return "ACTIVE".equals(status) && deletedAt == null;
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

  public String getStaffCode() {
    return staffCode;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getDesignation() {
    return designation;
  }

  public String getStatus() {
    return status;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
