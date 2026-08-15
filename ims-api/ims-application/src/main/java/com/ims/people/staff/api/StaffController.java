package com.ims.people.staff.api;

import com.ims.common.paging.PageResponse;
import com.ims.people.staff.application.StaffService;
import com.ims.people.staff.application.StaffService.CreateStaffCommand;
import com.ims.people.staff.application.StaffService.UpdateStaffCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/v1/staff")
@Tag(name = "Staff", description = "Tenant-scoped staff master data")
@SecurityRequirement(name = "bearer-jwt")
public class StaffController {

  private final StaffService staffService;

  public StaffController(StaffService staffService) {
    this.staffService = staffService;
  }

  public record CreateStaffRequest(
      @Size(max = 32) String staffCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String designation) {}

  public record UpdateStaffRequest(
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String designation) {}

  @GetMapping
  @Operation(summary = "List / search staff")
  @PreAuthorize("hasAuthority('staff:read')")
  public PageResponse<StaffResponse> list(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return staffService.list(q, status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get staff by id")
  @PreAuthorize("hasAuthority('staff:read')")
  public StaffResponse get(@PathVariable("id") Long id) {
    return staffService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create staff")
  @PreAuthorize("hasAuthority('staff:write')")
  public StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
    return staffService.create(
        new CreateStaffCommand(
            request.staffCode(),
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.email(),
            request.designation()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update staff (code immutable)")
  @PreAuthorize("hasAuthority('staff:write')")
  public StaffResponse update(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateStaffRequest request) {
    return staffService.update(
        id,
        new UpdateStaffCommand(
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.email(),
            request.designation()));
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Soft-deactivate staff")
  @PreAuthorize("hasAuthority('staff:write')")
  public StaffResponse deactivate(@PathVariable("id") Long id) {
    return staffService.deactivate(id);
  }
}
