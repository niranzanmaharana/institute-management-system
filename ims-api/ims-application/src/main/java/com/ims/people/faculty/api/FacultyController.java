package com.ims.people.faculty.api;

import com.ims.common.paging.PageResponse;
import com.ims.people.faculty.application.FacultyService;
import com.ims.people.faculty.application.FacultyService.AddressInput;
import com.ims.people.faculty.application.FacultyService.CreateFacultyCommand;
import com.ims.people.faculty.application.FacultyService.UpdateFacultyCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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
@RequestMapping("/api/v1/faculties")
@Tag(name = "Faculties", description = "Tenant-scoped faculty master data")
@SecurityRequirement(name = "bearer-jwt")
public class FacultyController {

  private final FacultyService facultyService;

  public FacultyController(FacultyService facultyService) {
    this.facultyService = facultyService;
  }

  public record AddressRequest(
      @NotBlank @Size(max = 255) String line1,
      @Size(max = 255) String line2,
      @NotBlank @Size(max = 64) String city,
      @Size(max = 64) String state,
      @Size(max = 16) String postalCode,
      @Size(max = 64) String country,
      boolean primaryAddress) {}

  public record CreateFacultyRequest(
      @Size(max = 32) String facultyCode,
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String department,
      List<AddressRequest> addresses) {}

  public record UpdateFacultyRequest(
      @NotBlank @Size(max = 64) String firstName,
      @NotBlank @Size(max = 64) String lastName,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      @Size(max = 128) String department,
      List<AddressRequest> addresses) {}

  @GetMapping
  @Operation(summary = "List / search faculties")
  @PreAuthorize("hasAuthority('faculty:read')")
  public PageResponse<FacultyResponse> list(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return facultyService.list(q, status, page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get faculty by id")
  @PreAuthorize("hasAuthority('faculty:read')")
  public FacultyResponse get(@PathVariable("id") Long id) {
    return facultyService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create faculty")
  @PreAuthorize("hasAuthority('faculty:write')")
  public FacultyResponse create(@Valid @RequestBody CreateFacultyRequest request) {
    return facultyService.create(toCreateCommand(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update faculty (code immutable)")
  @PreAuthorize("hasAuthority('faculty:write')")
  public FacultyResponse update(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateFacultyRequest request) {
    return facultyService.update(id, toUpdateCommand(request));
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Soft-deactivate faculty")
  @PreAuthorize("hasAuthority('faculty:write')")
  public FacultyResponse deactivate(@PathVariable("id") Long id) {
    return facultyService.deactivate(id);
  }

  private static CreateFacultyCommand toCreateCommand(CreateFacultyRequest request) {
    return new CreateFacultyCommand(
        request.facultyCode(),
        request.firstName(),
        request.lastName(),
        request.phone(),
        request.email(),
        request.department(),
        mapAddresses(request.addresses()));
  }

  private static UpdateFacultyCommand toUpdateCommand(UpdateFacultyRequest request) {
    return new UpdateFacultyCommand(
        request.firstName(),
        request.lastName(),
        request.phone(),
        request.email(),
        request.department(),
        mapAddresses(request.addresses()));
  }

  private static List<AddressInput> mapAddresses(List<AddressRequest> addresses) {
    if (addresses == null) {
      return null;
    }
    return addresses.stream()
        .map(
            a ->
                new AddressInput(
                    a.line1(),
                    a.line2(),
                    a.city(),
                    a.state(),
                    a.postalCode(),
                    a.country(),
                    a.primaryAddress()))
        .toList();
  }
}
