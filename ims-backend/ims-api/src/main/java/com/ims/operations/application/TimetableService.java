package com.ims.operations.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.BatchFacultyService;
import com.ims.academic.application.BatchFacultyService.SubjectView;
import com.ims.common.tenancy.TenantContext;
import com.ims.operations.api.TimetableDtos.BatchTimetableResponse;
import com.ims.operations.api.TimetableDtos.RoomResponse;
import com.ims.operations.api.TimetableDtos.TimetableSlotResponse;
import com.ims.operations.domain.Room;
import com.ims.operations.domain.TimetableSlot;
import com.ims.operations.infrastructure.RoomJpaRepository;
import com.ims.operations.infrastructure.TimetableSlotJpaRepository;
import com.ims.people.faculty.application.FacultyService;
import com.ims.people.faculty.application.FacultyService.FacultyRef;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TimetableService {

  private final RoomJpaRepository roomJpaRepository;
  private final TimetableSlotJpaRepository slotJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final BatchFacultyService batchFacultyService;
  private final FacultyService facultyService;
  private final CodeGeneratorService codeGeneratorService;
  private final AuditService auditService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public TimetableService(
      RoomJpaRepository roomJpaRepository,
      TimetableSlotJpaRepository slotJpaRepository,
      AcademicCatalogService academicCatalogService,
      BatchFacultyService batchFacultyService,
      FacultyService facultyService,
      CodeGeneratorService codeGeneratorService,
      AuditService auditService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.roomJpaRepository = roomJpaRepository;
    this.slotJpaRepository = slotJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.batchFacultyService = batchFacultyService;
    this.facultyService = facultyService;
    this.codeGeneratorService = codeGeneratorService;
    this.auditService = auditService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record CreateRoomCommand(String code, String name, int capacity) {}

  public record UpdateRoomCommand(String name, int capacity) {}

  public record UpsertSlotCommand(
      Long batchId,
      Long subjectId,
      Long facultyId,
      Long roomId,
      int dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {}

  @Transactional(readOnly = true)
  public List<RoomResponse> listRooms(boolean activeOnly) {
    long instituteId = requireTenant();
    List<Room> rows =
        activeOnly
            ? roomJpaRepository.findByInstituteIdAndStatusOrderByCodeAsc(
                instituteId, Room.STATUS_ACTIVE)
            : roomJpaRepository.findByInstituteIdOrderByCodeAsc(instituteId);
    return rows.stream().map(this::toRoom).toList();
  }

  @Transactional
  public RoomResponse createRoom(CreateRoomCommand command) {
    long instituteId = requireTenant();
    String code = codeGeneratorService.resolve(CodeEntityType.ROOM, command.code());
    if (roomJpaRepository.existsByInstituteIdAndCodeIgnoreCase(instituteId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Room code already exists");
    }
    Room saved =
        roomJpaRepository.save(Room.create(instituteId, code, command.name(), command.capacity()));
    auditService.record(
        instituteId,
        "operations",
        "Room",
        saved.getId(),
        "ROOM_CREATED",
        null,
        Map.of("code", saved.getCode(), "name", saved.getName()),
        null);
    return toRoom(saved);
  }

  @Transactional
  public RoomResponse updateRoom(Long id, UpdateRoomCommand command) {
    Room room = requireRoom(id);
    room.update(command.name(), command.capacity());
    return toRoom(roomJpaRepository.save(room));
  }

  @Transactional
  public RoomResponse deactivateRoom(Long id) {
    Room room = requireRoom(id);
    room.deactivate();
    return toRoom(roomJpaRepository.save(room));
  }

  @Transactional(readOnly = true)
  public BatchTimetableResponse batchTimetable(Long batchId, Integer dayOfWeek) {
    long instituteId = requireTenant();
    academicCatalogService.getBatch(batchId);
    List<TimetableSlot> slots;
    if (dayOfWeek != null) {
      if (dayOfWeek < 1 || dayOfWeek > 7) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek must be 1..7");
      }
      slots =
          slotJpaRepository.findByInstituteIdAndBatchIdAndDayOfWeekAndStatusOrderByStartTimeAsc(
              instituteId, batchId, dayOfWeek, TimetableSlot.STATUS_ACTIVE);
    } else {
      slots =
          slotJpaRepository.findByInstituteIdAndBatchIdAndStatusOrderByDayOfWeekAscStartTimeAsc(
              instituteId, batchId, TimetableSlot.STATUS_ACTIVE);
    }
    return new BatchTimetableResponse(batchId, slots.stream().map(this::toSlot).toList());
  }

  @Transactional
  public TimetableSlotResponse createSlot(UpsertSlotCommand command) {
    long instituteId = requireTenant();
    var batch = academicCatalogService.getBatch(command.batchId());
    Room room = requireActiveRoom(command.roomId());
    validateSubject(batch.courseId(), command.subjectId());
    if (command.facultyId() != null) {
      facultyService.requireActiveNamed(command.facultyId());
    }
    assertNoConflicts(
        instituteId,
        null,
        command.roomId(),
        command.facultyId(),
        command.dayOfWeek(),
        command.startTime(),
        command.endTime());
    TimetableSlot saved =
        slotJpaRepository.save(
            TimetableSlot.create(
                instituteId,
                command.batchId(),
                command.subjectId(),
                command.facultyId(),
                room.getId(),
                command.dayOfWeek(),
                command.startTime(),
                command.endTime()));
    auditService.record(
        instituteId,
        "operations",
        "TimetableSlot",
        saved.getId(),
        "TIMETABLE_SLOT_CREATED",
        null,
        Map.of(
            "batchId",
            saved.getBatchId(),
            "roomId",
            saved.getRoomId(),
            "dayOfWeek",
            saved.getDayOfWeek(),
            "startTime",
            saved.getStartTime().toString(),
            "endTime",
            saved.getEndTime().toString()),
        null);
    return toSlot(saved);
  }

  @Transactional
  public TimetableSlotResponse updateSlot(Long id, UpsertSlotCommand command) {
    TimetableSlot slot = requireSlot(id);
    var batch = academicCatalogService.getBatch(slot.getBatchId());
    if (command.batchId() != null && !command.batchId().equals(slot.getBatchId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId cannot be changed");
    }
    Room room = requireActiveRoom(command.roomId());
    validateSubject(batch.courseId(), command.subjectId());
    if (command.facultyId() != null) {
      facultyService.requireActiveNamed(command.facultyId());
    }
    assertNoConflicts(
        slot.getInstituteId(),
        slot.getId(),
        command.roomId(),
        command.facultyId(),
        command.dayOfWeek(),
        command.startTime(),
        command.endTime());
    slot.update(
        command.subjectId(),
        command.facultyId(),
        room.getId(),
        command.dayOfWeek(),
        command.startTime(),
        command.endTime());
    TimetableSlot saved = slotJpaRepository.save(slot);
    auditService.record(
        saved.getInstituteId(),
        "operations",
        "TimetableSlot",
        saved.getId(),
        "TIMETABLE_SLOT_UPDATED",
        null,
        Map.of("roomId", saved.getRoomId(), "dayOfWeek", saved.getDayOfWeek()),
        null);
    return toSlot(saved);
  }

  @Transactional
  public void cancelSlot(Long id) {
    TimetableSlot slot = requireSlot(id);
    slot.cancel();
    slotJpaRepository.save(slot);
    auditService.record(
        slot.getInstituteId(),
        "operations",
        "TimetableSlot",
        slot.getId(),
        "TIMETABLE_SLOT_CANCELLED",
        Map.of("status", TimetableSlot.STATUS_ACTIVE),
        Map.of("status", TimetableSlot.STATUS_CANCELLED),
        null);
  }

  private void assertNoConflicts(
      long instituteId,
      Long excludeSlotId,
      Long roomId,
      Long facultyId,
      int dayOfWeek,
      LocalTime start,
      LocalTime end) {
    List<TimetableSlot> roomSlots =
        slotJpaRepository.findByInstituteIdAndRoomIdAndDayOfWeekAndStatus(
            instituteId, roomId, dayOfWeek, TimetableSlot.STATUS_ACTIVE);
    for (TimetableSlot other : roomSlots) {
      if (excludeSlotId != null && excludeSlotId.equals(other.getId())) {
        continue;
      }
      if (other.overlaps(start, end)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Room conflict with slot #" + other.getId() + " (batch " + other.getBatchId() + ")");
      }
    }
    if (facultyId == null) {
      return;
    }
    List<TimetableSlot> facultySlots =
        slotJpaRepository.findByInstituteIdAndFacultyIdAndDayOfWeekAndStatus(
            instituteId, facultyId, dayOfWeek, TimetableSlot.STATUS_ACTIVE);
    for (TimetableSlot other : facultySlots) {
      if (excludeSlotId != null && excludeSlotId.equals(other.getId())) {
        continue;
      }
      if (other.overlaps(start, end)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Faculty conflict with slot #" + other.getId() + " (batch " + other.getBatchId() + ")");
      }
    }
  }

  private void validateSubject(Long courseId, Long subjectId) {
    if (subjectId == null) {
      return;
    }
    boolean ok =
        batchFacultyService.listSubjects(courseId).stream()
            .map(SubjectView::id)
            .anyMatch(subjectId::equals);
    if (!ok) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Subject does not belong to this batch's course");
    }
  }

  private Room requireActiveRoom(Long id) {
    Room room = requireRoom(id);
    if (!room.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is inactive");
    }
    return room;
  }

  private Room requireRoom(Long id) {
    long instituteId = requireTenant();
    return roomJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
  }

  private TimetableSlot requireSlot(Long id) {
    long instituteId = requireTenant();
    return slotJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
  }

  private RoomResponse toRoom(Room room) {
    return new RoomResponse(
        room.getId(), room.getCode(), room.getName(), room.getCapacity(), room.getStatus());
  }

  private TimetableSlotResponse toSlot(TimetableSlot slot) {
    Room room = requireRoom(slot.getRoomId());
    String subjectCode = null;
    String subjectName = null;
    if (slot.getSubjectId() != null) {
      var batch = academicCatalogService.getBatch(slot.getBatchId());
      SubjectView subject =
          batchFacultyService.listSubjects(batch.courseId()).stream()
              .filter(s -> s.id().equals(slot.getSubjectId()))
              .findFirst()
              .orElse(null);
      if (subject != null) {
        subjectCode = subject.code();
        subjectName = subject.name();
      }
    }
    String facultyCode = null;
    String facultyName = null;
    if (slot.getFacultyId() != null) {
      FacultyRef faculty =
          facultyService
              .findNamed(slot.getFacultyId())
              .orElse(new FacultyRef(slot.getFacultyId(), "?", "Unknown", "faculty", "UNKNOWN"));
      facultyCode = faculty.facultyCode();
      facultyName = (faculty.firstName() + " " + faculty.lastName()).trim();
    }
    return new TimetableSlotResponse(
        slot.getId(),
        slot.getBatchId(),
        slot.getSubjectId(),
        subjectCode,
        subjectName,
        slot.getFacultyId(),
        facultyCode,
        facultyName,
        room.getId(),
        room.getCode(),
        room.getName(),
        slot.getDayOfWeek(),
        slot.getStartTime(),
        slot.getEndTime(),
        slot.getStatus());
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }
}
