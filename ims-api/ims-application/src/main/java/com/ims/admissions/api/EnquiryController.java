package com.ims.admissions.api;

import com.ims.admissions.application.EnquiryService;
import com.ims.admissions.application.EnquiryService.CloseEnquiryCommand;
import com.ims.admissions.application.EnquiryService.ConvertEnquiryCommand;
import com.ims.admissions.application.EnquiryService.CreateEnquiryCommand;
import com.ims.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enquiries")
@Tag(name = "Enquiries", description = "Optional admission enquiries; convert to application")
@SecurityRequirement(name = "bearer-jwt")
public class EnquiryController {

  private final EnquiryService enquiryService;

  public EnquiryController(EnquiryService enquiryService) {
    this.enquiryService = enquiryService;
  }

  public record CreateEnquiryRequest(
      @NotBlank @Size(max = 128) String name,
      @Size(max = 32) String phone,
      @Email @Size(max = 255) String email,
      Long interestedCourseId,
      @Size(max = 1024) String notes) {}

  public record CloseEnquiryRequest(@Size(max = 1024) String notes) {}

  public record ConvertEnquiryRequest(@Size(max = 32) String applicationNo, Long courseId) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create OPEN enquiry")
  @PreAuthorize("hasAuthority('admission:write')")
  public EnquiryResponse create(@Valid @RequestBody CreateEnquiryRequest request) {
    return enquiryService.create(
        new CreateEnquiryCommand(
            request.name(),
            request.phone(),
            request.email(),
            request.interestedCourseId(),
            request.notes()));
  }

  @GetMapping
  @Operation(summary = "List / search enquiries")
  @PreAuthorize("hasAnyAuthority('admission:write', 'admission:approve', 'student:read')")
  public PageResponse<EnquiryResponse> list(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return enquiryService.list(q, status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get enquiry")
  @PreAuthorize("hasAnyAuthority('admission:write', 'admission:approve', 'student:read')")
  public EnquiryResponse get(@PathVariable("id") Long id) {
    return enquiryService.get(id);
  }

  @PostMapping("/{id}/close")
  @Operation(summary = "Close OPEN enquiry without converting")
  @PreAuthorize("hasAuthority('admission:write')")
  public EnquiryResponse close(
      @PathVariable("id") Long id, @Valid @RequestBody(required = false) CloseEnquiryRequest request) {
    CloseEnquiryRequest body = request == null ? new CloseEnquiryRequest(null) : request;
    return enquiryService.close(id, new CloseEnquiryCommand(body.notes()));
  }

  @PostMapping("/{id}/convert")
  @Operation(summary = "Convert OPEN enquiry into a SUBMITTED admission application")
  @PreAuthorize("hasAuthority('admission:write')")
  public AdmissionResponse convert(
      @PathVariable("id") Long id,
      @Valid @RequestBody(required = false) ConvertEnquiryRequest request) {
    ConvertEnquiryRequest body = request == null ? new ConvertEnquiryRequest(null, null) : request;
    return enquiryService.convert(
        id, new ConvertEnquiryCommand(body.applicationNo(), body.courseId()));
  }
}
