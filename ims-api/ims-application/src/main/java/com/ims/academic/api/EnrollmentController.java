package com.ims.academic.api;

import com.ims.academic.application.EnrollmentService;
import com.ims.academic.application.EnrollmentService.ActivateEnrollmentCommand;
import com.ims.academic.application.EnrollmentService.CreateEnrollmentCommand;
import com.ims.academic.application.EnrollmentService.ReasonCommand;
import com.ims.academic.application.EnrollmentService.TransferEnrollmentCommand;
import com.ims.finance.api.FinanceDtos.ActivationResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@Tag(name = "Enrollments", description = "Student enrollments and lifecycle")
@SecurityRequirement(name = "bearer-jwt")
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  public EnrollmentController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  public record CreateEnrollmentRequest(
      @NotNull Long studentId, @NotNull Long batchId, @NotNull Long feePlanId) {}

  public record ActivateEnrollmentRequest(
      boolean capacityOverride,
      @Size(max = 512) String overrideReason,
      boolean admissionWaiver,
      @Size(max = 512) String waiverReason) {}

  public record ReasonRequest(@NotBlank @Size(max = 512) String reason) {}

  public record TransferEnrollmentRequest(
      @NotNull Long targetBatchId,
      Long newFeePlanId,
      boolean capacityOverride,
      @Size(max = 512) String overrideReason) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create enrollment (APPLIED)")
  @PreAuthorize("hasAnyAuthority('course:write', 'enrollment:write')")
  public EnrollmentResponse create(@Valid @RequestBody CreateEnrollmentRequest request) {
    return enrollmentService.create(
        new CreateEnrollmentCommand(request.studentId(), request.batchId(), request.feePlanId()));
  }

  @GetMapping
  @Operation(summary = "List enrollments")
  @PreAuthorize("hasAnyAuthority('course:write', 'course:read', 'fee:collect')")
  public List<EnrollmentResponse> list() {
    return enrollmentService.list();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get enrollment")
  @PreAuthorize("hasAnyAuthority('course:write', 'course:read', 'fee:collect')")
  public EnrollmentResponse get(@PathVariable("id") Long id) {
    return enrollmentService.get(id);
  }

  @PostMapping("/{id}/activate")
  @Operation(summary = "Activate enrollment and create fee invoices")
  @PreAuthorize("hasAnyAuthority('course:write', 'fee:collect')")
  public ActivationResultResponse activate(
      @PathVariable("id") Long id,
      @Valid @RequestBody(required = false) ActivateEnrollmentRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    ActivateEnrollmentRequest body =
        request == null ? new ActivateEnrollmentRequest(false, null, false, null) : request;
    return enrollmentService.activate(
        id,
        idempotencyKey,
        new ActivateEnrollmentCommand(
            body.capacityOverride(),
            body.overrideReason(),
            body.admissionWaiver(),
            body.waiverReason()));
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel APPLIED enrollment")
  @PreAuthorize("hasAuthority('course:write')")
  public EnrollmentResponse cancel(
      @PathVariable("id") Long id, @Valid @RequestBody ReasonRequest request) {
    return enrollmentService.cancel(id, new ReasonCommand(request.reason()));
  }

  @PostMapping("/{id}/suspend")
  @Operation(summary = "Suspend ACTIVE enrollment")
  @PreAuthorize("hasAuthority('course:write')")
  public EnrollmentResponse suspend(
      @PathVariable("id") Long id, @Valid @RequestBody ReasonRequest request) {
    return enrollmentService.suspend(id, new ReasonCommand(request.reason()));
  }

  @PostMapping("/{id}/resume")
  @Operation(summary = "Resume SUSPENDED enrollment to ACTIVE")
  @PreAuthorize("hasAuthority('course:write')")
  public EnrollmentResponse resume(
      @PathVariable("id") Long id,
      @Valid @RequestBody(required = false) ActivateEnrollmentRequest request) {
    ActivateEnrollmentRequest body =
        request == null ? new ActivateEnrollmentRequest(false, null, false, null) : request;
    return enrollmentService.resume(
        id,
        new ActivateEnrollmentCommand(
            body.capacityOverride(),
            body.overrideReason(),
            body.admissionWaiver(),
            body.waiverReason()));
  }

  @PostMapping("/{id}/withdraw")
  @Operation(summary = "Withdraw ACTIVE or SUSPENDED enrollment")
  @PreAuthorize("hasAuthority('course:write')")
  public EnrollmentResponse withdraw(
      @PathVariable("id") Long id, @Valid @RequestBody ReasonRequest request) {
    return enrollmentService.withdraw(id, new ReasonCommand(request.reason()));
  }

  @PostMapping("/{id}/complete")
  @Operation(summary = "Complete ACTIVE enrollment")
  @PreAuthorize("hasAuthority('course:write')")
  public EnrollmentResponse complete(
      @PathVariable("id") Long id, @Valid @RequestBody ReasonRequest request) {
    return enrollmentService.complete(id, new ReasonCommand(request.reason()));
  }

  @PostMapping("/{id}/transfer")
  @Operation(summary = "Transfer ACTIVE enrollment to another batch (same course)")
  @PreAuthorize("hasAuthority('course:write')")
  public EnrollmentResponse transfer(
      @PathVariable("id") Long id, @Valid @RequestBody TransferEnrollmentRequest request) {
    return enrollmentService.transfer(
        id,
        new TransferEnrollmentCommand(
            request.targetBatchId(),
            request.newFeePlanId(),
            request.capacityOverride(),
            request.overrideReason()));
  }
}
