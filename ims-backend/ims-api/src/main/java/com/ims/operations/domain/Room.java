package com.ims.operations.domain;

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
@Table(name = "rooms")
@Filter(name = "instituteFilter", condition = "institute_id = :instituteId")
public class Room {

  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_INACTIVE = "INACTIVE";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(nullable = false)
  private int capacity;

  @Column(nullable = false, length = 32)
  private String status = STATUS_ACTIVE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Room() {}

  public static Room create(Long instituteId, String code, String name, int capacity) {
    Room row = new Room();
    row.instituteId = instituteId;
    row.code = normalizeCode(code);
    row.name = requireName(name);
    row.capacity = requireCapacity(capacity);
    row.status = STATUS_ACTIVE;
    Instant now = Instant.now();
    row.createdAt = now;
    row.updatedAt = now;
    return row;
  }

  public void update(String name, int capacity) {
    this.name = requireName(name);
    this.capacity = requireCapacity(capacity);
    this.updatedAt = Instant.now();
  }

  public void deactivate() {
    this.status = STATUS_INACTIVE;
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status);
  }

  private static String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code is required");
    }
    return code.trim().toUpperCase();
  }

  private static String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required");
    }
    return name.trim();
  }

  private static int requireCapacity(int capacity) {
    if (capacity < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacity cannot be negative");
    }
    return capacity;
  }

  public Long getId() {
    return id;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getCapacity() {
    return capacity;
  }

  public String getStatus() {
    return status;
  }
}
