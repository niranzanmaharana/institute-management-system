package com.ims.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "timetable_slots")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class TimetableSlot {

  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "subject_id")
  private Long subjectId;

  @Column(name = "faculty_id")
  private Long facultyId;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "day_of_week", nullable = false)
  private int dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(nullable = false, length = 32)
  private String status = STATUS_ACTIVE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected TimetableSlot() {}

  public static TimetableSlot create(
      Long instituteId,
      Long batchId,
      Long subjectId,
      Long facultyId,
      Long roomId,
      int dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {
    TimetableSlot row = new TimetableSlot();
    row.instituteId = instituteId;
    row.batchId = batchId;
    row.subjectId = subjectId;
    row.facultyId = facultyId;
    row.roomId = roomId;
    row.dayOfWeek = requireDay(dayOfWeek);
    row.startTime = startTime;
    row.endTime = endTime;
    validateTimes(startTime, endTime);
    row.status = STATUS_ACTIVE;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void update(
      Long subjectId,
      Long facultyId,
      Long roomId,
      int dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {
    requireActive();
    this.subjectId = subjectId;
    this.facultyId = facultyId;
    this.roomId = roomId;
    this.dayOfWeek = requireDay(dayOfWeek);
    validateTimes(startTime, endTime);
    this.startTime = startTime;
    this.endTime = endTime;
    this.updatedAt = Instant.now();
  }

  public void cancel() {
    requireActive();
    this.status = STATUS_CANCELLED;
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
  }

  public boolean overlaps(LocalTime otherStart, LocalTime otherEnd) {
    return startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
  }

  private void requireActive() {
    if (!isActive()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is cancelled");
    }
  }

  private static int requireDay(int dayOfWeek) {
    if (dayOfWeek < 1 || dayOfWeek > 7) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "dayOfWeek must be 1 (Mon) through 7 (Sun)");
    }
    return dayOfWeek;
  }

  private static void validateTimes(LocalTime start, LocalTime end) {
    if (start == null || end == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime are required");
    }
    if (!end.isAfter(start)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
    }
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public Long getFacultyId() {
    return facultyId;
  }

  public Long getRoomId() {
    return roomId;
  }

  public int getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public String getStatus() {
    return status;
  }
}
