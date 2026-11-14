package com.ims.operations.api;

import com.ims.operations.api.TimetableDtos.BatchTimetableResponse;
import com.ims.operations.api.TimetableDtos.RoomResponse;
import com.ims.operations.api.TimetableDtos.TimetableSlotResponse;
import com.ims.operations.application.TimetableService;
import com.ims.operations.application.TimetableService.CreateRoomCommand;
import com.ims.operations.application.TimetableService.UpdateRoomCommand;
import com.ims.operations.application.TimetableService.UpsertSlotCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Timetable", description = "Rooms and weekly timetable slots")
@SecurityRequirement(name = "bearer-jwt")
public class TimetableController {

  private final TimetableService timetableService;

  public TimetableController(TimetableService timetableService) {
    this.timetableService = timetableService;
  }

  public record CreateRoomRequest(
      @Size(max = 32) String code,
      @NotBlank @Size(max = 128) String name,
      @Min(0) int capacity) {}

  public record UpdateRoomRequest(@NotBlank @Size(max = 128) String name, @Min(0) int capacity) {}

  public record UpsertSlotRequest(
      @NotNull Long batchId,
      Long subjectId,
      Long facultyId,
      @NotNull Long roomId,
      @NotNull @Min(1) @Max(7) Integer dayOfWeek,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {}

  @GetMapping("/rooms")
  @Operation(summary = "List rooms")
  @PreAuthorize(
      "hasAuthority('timetable:write') or hasAuthority('course:read') or hasAuthority('attendance:mark')")
  public List<RoomResponse> listRooms(
      @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
    return timetableService.listRooms(activeOnly);
  }

  @PostMapping("/rooms")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create room")
  @PreAuthorize("hasAuthority('timetable:write')")
  public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
    return timetableService.createRoom(
        new CreateRoomCommand(request.code(), request.name(), request.capacity()));
  }

  @PutMapping("/rooms/{id}")
  @Operation(summary = "Update room")
  @PreAuthorize("hasAuthority('timetable:write')")
  public RoomResponse updateRoom(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateRoomRequest request) {
    return timetableService.updateRoom(id, new UpdateRoomCommand(request.name(), request.capacity()));
  }

  @PostMapping("/rooms/{id}/deactivate")
  @Operation(summary = "Deactivate room")
  @PreAuthorize("hasAuthority('timetable:write')")
  public RoomResponse deactivateRoom(@PathVariable("id") Long id) {
    return timetableService.deactivateRoom(id);
  }

  @GetMapping("/timetable/batches/{batchId}")
  @Operation(summary = "Batch weekly timetable (optional dayOfWeek 1-7)")
  @PreAuthorize(
      "hasAuthority('timetable:write') or hasAuthority('course:read') or hasAuthority('attendance:mark')")
  public BatchTimetableResponse batchTimetable(
      @PathVariable("batchId") Long batchId,
      @RequestParam(required = false) Integer dayOfWeek) {
    return timetableService.batchTimetable(batchId, dayOfWeek);
  }

  @PostMapping("/timetable/slots")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create timetable slot (rejects room/faculty conflicts)")
  @PreAuthorize("hasAuthority('timetable:write')")
  public TimetableSlotResponse createSlot(@Valid @RequestBody UpsertSlotRequest request) {
    return timetableService.createSlot(toCommand(request));
  }

  @PutMapping("/timetable/slots/{id}")
  @Operation(summary = "Update timetable slot")
  @PreAuthorize("hasAuthority('timetable:write')")
  public TimetableSlotResponse updateSlot(
      @PathVariable("id") Long id, @Valid @RequestBody UpsertSlotRequest request) {
    return timetableService.updateSlot(id, toCommand(request));
  }

  @PostMapping("/timetable/slots/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Cancel timetable slot")
  @PreAuthorize("hasAuthority('timetable:write')")
  public void cancelSlot(@PathVariable("id") Long id) {
    timetableService.cancelSlot(id);
  }

  private static UpsertSlotCommand toCommand(UpsertSlotRequest request) {
    return new UpsertSlotCommand(
        request.batchId(),
        request.subjectId(),
        request.facultyId(),
        request.roomId(),
        request.dayOfWeek(),
        request.startTime(),
        request.endTime());
  }
}
