package com.ims.identity.api;

import com.ims.identity.application.UserManagementService;
import com.ims.identity.application.UserManagementService.CreateUserCommand;
import com.ims.identity.application.UserManagementService.UpdateUserCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Users", description = "Institute user and role management")
@SecurityRequirement(name = "bearer-jwt")
public class UserManagementController {

  private final UserManagementService userManagementService;

  public UserManagementController(UserManagementService userManagementService) {
    this.userManagementService = userManagementService;
  }

  public record CreateUserRequest(
      @NotBlank @Size(max = 64) String username,
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotEmpty List<@NotBlank String> roles) {}

  public record UpdateUserRequest(
      @NotBlank @Email @Size(max = 255) String email,
      @NotEmpty List<@NotBlank String> roles,
      @Size(min = 8, max = 72) String password) {}

  @GetMapping("/roles")
  @Operation(summary = "List roles assignable within an institute")
  @PreAuthorize("hasAuthority('user:manage')")
  public List<RoleResponse> listRoles() {
    return userManagementService.listAssignableRoles();
  }

  @GetMapping("/users")
  @Operation(summary = "List institute users")
  @PreAuthorize("hasAuthority('user:manage')")
  public List<ManagedUserResponse> listUsers() {
    return userManagementService.list();
  }

  @GetMapping("/users/{id}")
  @Operation(summary = "Get institute user")
  @PreAuthorize("hasAuthority('user:manage')")
  public ManagedUserResponse getUser(@PathVariable("id") Long id) {
    return userManagementService.get(id);
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create institute user with roles")
  @PreAuthorize("hasAuthority('user:manage')")
  public ManagedUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    return userManagementService.create(
        new CreateUserCommand(
            request.username(), request.email(), request.password(), request.roles()));
  }

  @PutMapping("/users/{id}")
  @Operation(summary = "Update user email, roles, optional password")
  @PreAuthorize("hasAuthority('user:manage')")
  public ManagedUserResponse updateUser(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateUserRequest request) {
    return userManagementService.update(
        id, new UpdateUserCommand(request.email(), request.roles(), request.password()));
  }

  @PostMapping("/users/{id}/deactivate")
  @Operation(summary = "Deactivate institute user")
  @PreAuthorize("hasAuthority('user:manage')")
  public ManagedUserResponse deactivate(@PathVariable("id") Long id) {
    return userManagementService.deactivate(id);
  }

  @PostMapping("/users/{id}/activate")
  @Operation(summary = "Activate institute user")
  @PreAuthorize("hasAuthority('user:manage')")
  public ManagedUserResponse activate(@PathVariable("id") Long id) {
    return userManagementService.activate(id);
  }
}
