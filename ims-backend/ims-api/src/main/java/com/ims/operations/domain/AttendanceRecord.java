package com.ims.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import org.hibernate.annotations.Filter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "attendance_records")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class AttendanceRecord {

  public static final String STATUS_PRESENT = "PRESENT";
  public static final String STATUS_ABSENT = "ABSENT";
  public static final String STATUS_LATE = "LATE";
  public static final String STATUS_EXCUSED = "EXCUSED";

  private static final Set<String> ALLOWED =
      Set.of(STATUS_PRESENT, STATUS_ABSENT, STATUS_LATE, STATUS_EXCUSED);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(length = 255)
  private String note;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected AttendanceRecord() {}

  public static AttendanceRecord create(
      Long instituteId, Long sessionId, Long studentId, String status, String note) {
    AttendanceRecord row = new AttendanceRecord();
    row.instituteId = instituteId;
    row.sessionId = sessionId;
    row.studentId = studentId;
    row.status = normalizeStatus(status);
    row.note = blankToNull(note);
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void update(String status, String note) {
    this.status = normalizeStatus(status);
    this.note = blankToNull(note);
    this.updatedAt = Instant.now();
  }

  public static boolean isPresentEffective(String status) {
    return STATUS_PRESENT.equals(status) || STATUS_LATE.equals(status);
  }

  public static boolean isAbsent(String status) {
    return STATUS_ABSENT.equals(status);
  }

  public static boolean isExcused(String status) {
    return STATUS_EXCUSED.equals(status);
  }

  private static String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance status is required");
    }
    String normalized = status.trim().toUpperCase();
    if (!ALLOWED.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance status");
    }
    return normalized;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public Long getStudentId() {
    return studentId;
  }

  public String getStatus() {
    return status;
  }

  public String getNote() {
    return note;
  }
}
