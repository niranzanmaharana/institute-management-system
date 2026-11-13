package com.ims.people.faculty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "faculties")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Faculty {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "faculty_code", nullable = false, length = 32)
  private String facultyCode;

  @Column(name = "first_name", nullable = false, length = 64)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 64)
  private String lastName;

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(length = 128)
  private String department;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Faculty() {}

  public static Faculty create(
      Long instituteId,
      String facultyCode,
      String firstName,
      String lastName,
      String phone,
      String email,
      String department) {
    Faculty faculty = new Faculty();
    faculty.instituteId = instituteId;
    faculty.facultyCode = facultyCode.trim().toUpperCase();
    faculty.firstName = firstName.trim();
    faculty.lastName = lastName.trim();
    faculty.phone = blankToNull(phone);
    faculty.email = blankToNull(email);
    faculty.department = blankToNull(department);
    faculty.status = "ACTIVE";
    Instant now = Instant.now();
    faculty.createdAt = now;
    faculty.updatedAt = now;
    return faculty;
  }

  public void update(
      String firstName, String lastName, String phone, String email, String department) {
    this.firstName = firstName.trim();
    this.lastName = lastName.trim();
    this.phone = blankToNull(phone);
    this.email = blankToNull(email);
    this.department = blankToNull(department);
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

  public String getFacultyCode() {
    return facultyCode;
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

  public String getDepartment() {
    return department;
  }

  public String getStatus() {
    return status;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
