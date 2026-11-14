package com.ims.operations.api;

import com.ims.operations.api.AttendanceDtos.AttendanceSessionResponse;
import com.ims.operations.api.AttendanceDtos.BatchAttendanceSummary;
import com.ims.operations.api.AttendanceDtos.StudentAttendanceSummary;
import com.ims.operations.application.AttendanceService;
import com.ims.operations.application.AttendanceService.CreateSessionCommand;
import com.ims.operations.application.AttendanceService.MarkRecordCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Attendance", description = "Attendance sessions and summaries")
@SecurityRequirement(name = "bearer-jwt")
public class AttendanceController {

  private final AttendanceService attendanceService;

  public AttendanceController(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  public record CreateSessionRequest(
      @NotNull Long batchId,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
      Long subjectId) {}

  public record MarkRequest(@NotEmpty List<@Valid MarkItem> records) {}

  public record MarkItem(
      @NotNull Long studentId,
      @NotBlank @Size(max = 32) String status,
      @Size(max = 255) String note) {}

  public record UnlockRequest(@NotBlank @Size(min = 5, max = 512) String reason) {}

  @PostMapping("/attendance/sessions")
  @Operation(summary = "Create attendance session (DRAFT)")
  @PreAuthorize("hasAuthority('attendance:mark')")
  public AttendanceSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
    return attendanceService.create(
        new CreateSessionCommand(request.batchId(), request.sessionDate(), request.subjectId()));
  }

  @GetMapping("/attendance/sessions")
  @Operation(summary = "List attendance sessions")
  @PreAuthorize("hasAuthority('attendance:mark') or hasAuthority('attendance:unlock')")
  public List<AttendanceSessionResponse> list(
      @RequestParam(required = false) Long batchId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return attendanceService.list(batchId, from, to);
  }

  @GetMapping("/attendance/sessions/{id}")
  @Operation(summary = "Get attendance session with roster")
  @PreAuthorize("hasAuthority('attendance:mark') or hasAuthority('attendance:unlock')")
  public AttendanceSessionResponse get(@PathVariable("id") Long id) {
    return attendanceService.get(id);
  }

  @PutMapping("/attendance/sessions/{id}/records")
  @Operation(summary = "Mark attendance records")
  @PreAuthorize("hasAuthority('attendance:mark')")
  public AttendanceSessionResponse mark(
      @PathVariable("id") Long id, @Valid @RequestBody MarkRequest request) {
    List<MarkRecordCommand> marks =
        request.records().stream()
            .map(r -> new MarkRecordCommand(r.studentId(), r.status(), r.note()))
            .toList();
    return attendanceService.mark(id, marks);
  }

  @PostMapping("/attendance/sessions/{id}/submit")
  @Operation(summary = "Submit DRAFT session")
  @PreAuthorize("hasAuthority('attendance:mark')")
  public AttendanceSessionResponse submit(@PathVariable("id") Long id) {
    return attendanceService.submit(id);
  }

  @PostMapping("/attendance/sessions/{id}/lock")
  @Operation(summary = "Lock SUBMITTED session")
  @PreAuthorize("hasAuthority('attendance:mark')")
  public AttendanceSessionResponse lock(@PathVariable("id") Long id) {
    return attendanceService.lock(id);
  }

  @PostMapping("/attendance/sessions/{id}/unlock")
  @Operation(summary = "Unlock LOCKED session (audited)")
  @PreAuthorize("hasAuthority('attendance:unlock')")
  public AttendanceSessionResponse unlock(
      @PathVariable("id") Long id, @Valid @RequestBody UnlockRequest request) {
    return attendanceService.unlock(id, request.reason());
  }

  @GetMapping("/attendance/students/{studentId}/summary")
  @Operation(summary = "Student attendance % for a batch (RP-AT-01)")
  @PreAuthorize(
      "hasAuthority('attendance:mark') or hasAuthority('attendance:unlock') or hasRole('STUDENT')")
  public StudentAttendanceSummary studentSummary(
      @PathVariable("studentId") Long studentId, @RequestParam Long batchId) {
    return attendanceService.studentSummary(studentId, batchId);
  }

  @GetMapping("/attendance/batches/{batchId}/summary")
  @Operation(summary = "Batch attendance summary (RP-AT-02)")
  @PreAuthorize("hasAuthority('attendance:mark') or hasAuthority('attendance:unlock')")
  public BatchAttendanceSummary batchSummary(@PathVariable("batchId") Long batchId) {
    return attendanceService.batchSummary(batchId);
  }
}
