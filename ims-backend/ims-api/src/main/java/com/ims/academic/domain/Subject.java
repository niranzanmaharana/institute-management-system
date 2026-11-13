package com.ims.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "subjects")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Subject {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Subject() {}

  public static Subject create(Long instituteId, Long courseId, String code, String name) {
    Subject subject = new Subject();
    subject.instituteId = instituteId;
    subject.courseId = courseId;
    subject.code = code.trim().toUpperCase();
    subject.name = name.trim();
    subject.status = "ACTIVE";
    Instant now = Instant.now();
    subject.createdAt = now;
    subject.updatedAt = now;
    return subject;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getStatus() {
    return status;
  }
}
