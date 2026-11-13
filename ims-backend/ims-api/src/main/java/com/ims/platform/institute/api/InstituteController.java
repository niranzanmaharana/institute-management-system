package com.ims.platform.institute.api;

import com.ims.identity.security.ImsUserPrincipal;
import com.ims.platform.institute.application.InstituteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institutes")
@Tag(name = "Institutes", description = "Platform institute directory")
@SecurityRequirement(name = "bearer-jwt")
public class InstituteController {

  private final InstituteService instituteService;

  public InstituteController(InstituteService instituteService) {
    this.instituteService = instituteService;
  }

  public record InitialAdminRequest(
      @NotBlank @Size(max = 64) String username,
      @NotBlank @Email @Size(max = 255) String email,
      @Size(min = 8, max = 128) String password) {}

  /** Contact / branding profile shared by create and update. */
  public record InstituteProfileRequest(
      @Size(max = 32) String mobile,
      @Size(max = 255) String adminEmail,
      @Size(max = 512) String website,
      @Size(max = 255) String addressLine1,
      @Size(max = 255) String addressLine2,
      @Size(max = 128) String city,
      @Size(max = 128) String state,
      @Size(max = 32) String postalCode,
      @Size(max = 128) String country,
      @Size(max = 1024) String iconUrl) {}

  public record CreateInstituteRequest(
      @Size(max = 32) String code,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 64) String timezone,
      @Valid InstituteProfileRequest profile,
      @Valid InitialAdminRequest initialAdmin) {}

  public record UpdateInstituteRequest(
      @NotBlank @Size(max = 255) String name,
      @Size(max = 64) String timezone,
      @Valid InstituteProfileRequest profile) {}

  @GetMapping
  @Operation(summary = "List institutes (platform: all; institute admin: own only)")
  @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
  public List<InstituteResponse> list(@AuthenticationPrincipal ImsUserPrincipal principal) {
    return instituteService.listVisible(principal == null ? null : principal.getInstituteId());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get institute by id (platform: any; institute admin: own only)")
  @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
  public InstituteResponse get(
      @PathVariable("id") Long id, @AuthenticationPrincipal ImsUserPrincipal principal) {
    return instituteService.getVisible(id, principal);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create institute (PLATFORM_ADMIN)",
      description =
          "Include profile (mobile, admin email, website, address, icon URL) and optional initialAdmin.")
  @PreAuthorize("hasAuthority('institute:manage')")
  public InstituteResponse create(@Valid @RequestBody CreateInstituteRequest request) {
    return instituteService.create(request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update institute profile (PLATFORM_ADMIN: any; ADMIN: own institute)")
  @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
  public InstituteResponse update(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateInstituteRequest request,
      @AuthenticationPrincipal ImsUserPrincipal principal) {
    return instituteService.update(id, request, principal);
  }

  @PostMapping("/{id}/suspend")
  @Operation(summary = "Suspend institute (PLATFORM_ADMIN)")
  @PreAuthorize("hasAuthority('institute:manage')")
  public InstituteResponse suspend(@PathVariable("id") Long id) {
    return instituteService.suspend(id);
  }
}
