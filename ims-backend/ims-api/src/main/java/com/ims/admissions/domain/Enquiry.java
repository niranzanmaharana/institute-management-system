package com.ims.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "enquiries")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Enquiry {

  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_CONVERTED = "CONVERTED";
  public static final String STATUS_CLOSED = "CLOSED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(name = "interested_course_id")
  private Long interestedCourseId;

  @Column(nullable = false, length = 32)
  private String status = STATUS_OPEN;

  @Column(length = 1024)
  private String notes;

  @Column(name = "converted_application_id")
  private Long convertedApplicationId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Enquiry() {}

  public static Enquiry create(
      Long instituteId,
      String name,
      String phone,
      String email,
      Long interestedCourseId,
      String notes) {
    Enquiry enquiry = new Enquiry();
    enquiry.instituteId = instituteId;
    enquiry.name = name.trim();
    enquiry.phone = blankToNull(phone);
    enquiry.email = blankToNull(email);
    enquiry.interestedCourseId = interestedCourseId;
    enquiry.notes = blankToNull(notes);
    enquiry.status = STATUS_OPEN;
    Instant now = Instant.now();
    enquiry.createdAt = now;
    enquiry.updatedAt = now;
    return enquiry;
  }

  public void close(String notes) {
    requireOpen("Only OPEN enquiries can be closed");
    this.status = STATUS_CLOSED;
    if (notes != null && !notes.isBlank()) {
      this.notes = notes.trim();
    }
    this.updatedAt = Instant.now();
  }

  public void convert(Long applicationId) {
    requireOpen("Only OPEN enquiries can be converted");
    this.status = STATUS_CONVERTED;
    this.convertedApplicationId = applicationId;
    this.updatedAt = Instant.now();
  }

  private void requireOpen(String message) {
    if (!STATUS_OPEN.equals(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
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

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public Long getInterestedCourseId() {
    return interestedCourseId;
  }

  public String getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public Long getConvertedApplicationId() {
    return convertedApplicationId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
