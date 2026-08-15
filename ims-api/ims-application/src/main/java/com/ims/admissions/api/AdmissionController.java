package com.ims.admissions.api;

import com.ims.admissions.application.AdmissionService;
import com.ims.admissions.application.AdmissionService.ApproveAdmissionCommand;
import com.ims.admissions.application.AdmissionService.CreateAdmissionCommand;
import com.ims.admissions.application.AdmissionService.CreateStudentOnApprove;
import com.ims.admissions.application.AdmissionService.ReasonCommand;
import com.ims.common.paging.PageResponse;
import com.ims.identity.security.ImsUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admissions")
@Tag(name = "Admissions", description = "Admission applications approve/reject/cancel")
@SecurityRequirement(name = "bearer-jwt")
public class AdmissionController {

  private final AdmissionService admissionService;

  public AdmissionController(AdmissionService admissionService) {
    this.admissionService = admissionService;
  }

  public record CreateAdmissionRequest(
      @Size(max = 32) String applicationNo,
      @NotNull Long courseId,
      @NotBlank @Size(max = 128) String applicantName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      Long enquiryId) {}

  public record CreateStudentRequest(
      @Size(max = 32) String studentCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Email @Size(max = 255) String email) {}

  public record ApproveAdmissionRequest(
      Long studentId, @Valid CreateStudentRequest createStudent, @Size(max = 512) String reason) {}

  public record ReasonRequest(@NotBlank @Size(min = 5, max = 512) String reason) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create admission application (SUBMITTED)")
  @PreAuthorize("hasAuthority('admission:write')")
  public AdmissionResponse create(@Valid @RequestBody CreateAdmissionRequest request) {
    return admissionService.create(
        new CreateAdmissionCommand(
            request.applicationNo(),
            request.courseId(),
            request.applicantName(),
            request.phone(),
            request.email(),
            request.enquiryId()));
  }

  @GetMapping
  @Operation(summary = "List / search admission applications")
  @PreAuthorize("hasAnyAuthority('admission:write', 'admission:approve', 'student:read')")
  public PageResponse<AdmissionResponse> list(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return admissionService.list(q, status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get admission application")
  @PreAuthorize("hasAnyAuthority('admission:write', 'admission:approve', 'student:read')")
  public AdmissionResponse get(@PathVariable("id") Long id) {
    return admissionService.get(id);
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Approve admission (link or create student)")
  @PreAuthorize("hasAuthority('admission:approve')")
  public AdmissionResponse approve(
      @PathVariable("id") Long id,
      @Valid @RequestBody(required = false) ApproveAdmissionRequest request,
      @AuthenticationPrincipal ImsUserPrincipal principal) {
    ApproveAdmissionRequest body =
        request == null ? new ApproveAdmissionRequest(null, null, null) : request;
    CreateStudentOnApprove createStudent =
        body.createStudent() == null
            ? null
            : new CreateStudentOnApprove(
                body.createStudent().studentCode(),
                body.createStudent().firstName(),
                body.createStudent().lastName(),
                body.createStudent().phone(),
                body.createStudent().email());
    return admissionService.approve(
        id,
        new ApproveAdmissionCommand(body.studentId(), createStudent, body.reason()),
        principal.getUserId());
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Reject admission")
  @PreAuthorize("hasAuthority('admission:approve')")
  public AdmissionResponse reject(
      @PathVariable("id") Long id,
      @Valid @RequestBody ReasonRequest request,
      @AuthenticationPrincipal ImsUserPrincipal principal) {
    return admissionService.reject(
        id, new ReasonCommand(request.reason()), principal.getUserId());
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel SUBMITTED admission")
  @PreAuthorize("hasAuthority('admission:write')")
  public AdmissionResponse cancel(
      @PathVariable("id") Long id,
      @Valid @RequestBody ReasonRequest request,
      @AuthenticationPrincipal ImsUserPrincipal principal) {
    return admissionService.cancel(
        id, new ReasonCommand(request.reason()), principal.getUserId());
  }
}
